package com.aba.os.abaosserver.service;

import com.aba.os.abaosserver.domain.*;
import com.aba.os.abaosserver.domain.Report.ReportType;
import com.aba.os.abaosserver.dto.report.AiReportContent;
import com.aba.os.abaosserver.dto.report.ReportCreateRequest;
import com.aba.os.abaosserver.dto.report.ReportListResponse;
import com.aba.os.abaosserver.dto.report.ReportResponse;
import com.aba.os.abaosserver.repository.ChildRepository;
import com.aba.os.abaosserver.repository.GoalRepository;
import com.aba.os.abaosserver.repository.ReportRepository;
import com.aba.os.abaosserver.repository.SessionRepository;
import com.aba.os.abaosserver.security.SecurityUtil;
import com.aba.os.abaosserver.service.OpenAiService.GoalDetail;
import com.aba.os.abaosserver.service.OpenAiService.SessionScore;
import com.aba.os.abaosserver.service.OpenAiService.StructuredReportContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ReportService {

    private final ReportRepository reportRepository;
    private final SessionRepository sessionRepository;
    private final ChildRepository childRepository;
    private final GoalRepository goalRepository;
    private final SecurityUtil securityUtil;
    private final OpenAiService openAiService;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 리포트 생성
     * - 기간 내 세션 데이터 집계
     * - 목표별 달성률 계산
     * - PARENT_SUMMARY 타입인 경우 AI 생성
     */
    @Transactional
    public ReportResponse createReport(ReportCreateRequest request) {
        Long centerId = securityUtil.getCurrentCenterId();
        ReportType reportType = request.getReportType() != null
                ? request.getReportType()
                : ReportType.PARENT_SUMMARY;

        log.info("리포트 생성 시작 - 아동: {}, 타입: {}, 기간: {} ~ {}",
                request.getChildId(), reportType, request.getPeriodStart(), request.getPeriodEnd());

        // 날짜 검증
        if (request.getPeriodEnd().isBefore(request.getPeriodStart())) {
            throw new IllegalArgumentException("종료 날짜는 시작 날짜 이후여야 합니다.");
        }

        // 아동 조회 및 검증
        Child child = childRepository.findById(request.getChildId())
                .orElseThrow(() -> new IllegalArgumentException("아동을 찾을 수 없습니다."));

        if (!child.getCenter().getId().equals(centerId)) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }

        // 기간 내 세션 조회 (trials 포함)
        List<Session> sessions = sessionRepository.findAllByChildIdAndSessionDateBetween(
                request.getChildId(),
                request.getPeriodStart(),
                request.getPeriodEnd()
        );

        // 전체 통계 집계
        StatisticsResult stats = calculateStatistics(sessions);

        // 목표별 상세 통계 계산
        List<GoalDetail> goalDetails = calculateGoalDetails(request.getChildId(), sessions);

        // 세션 노트 수집
        String combinedNotes = sessions.stream()
                .filter(s -> s.getNotes() != null && !s.getNotes().isBlank())
                .map(Session::getNotes)
                .limit(5)
                .collect(Collectors.joining("; "));

        // 리포트 내용 생성 (타입에 따라 분기)
        String content = generateReportContent(
                reportType,
                child,
                request.getPeriodStart(),
                request.getPeriodEnd(),
                stats,
                goalDetails,
                combinedNotes,
                sessions  // 세션 목록 전달 (차트 데이터용)
        );

        // Report 엔티티 생성 및 저장
        Report report = Report.builder()
                .child(child)
                .title(request.getTitle())
                .periodStart(request.getPeriodStart())
                .periodEnd(request.getPeriodEnd())
                .totalSessions(stats.totalSessions)
                .totalTrials(stats.totalTrials)
                .totalSuccesses(stats.totalSuccesses)
                .averageAccuracy(stats.averageAccuracy)
                .reportType(reportType)
                .content(content)
                .build();

        Report savedReport = reportRepository.save(report);
        log.info("리포트 생성 완료 - ID: {}, 타입: {}", savedReport.getId(), reportType);

        return ReportResponse.from(savedReport);
    }

    /**
     * 아동별 리포트 목록 조회
     */
    public List<ReportListResponse> getReports(Long childId) {
        Long centerId = securityUtil.getCurrentCenterId();

        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("아동을 찾을 수 없습니다."));

        if (!child.getCenter().getId().equals(centerId)) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }

        List<Report> reports = reportRepository.findByChildIdOrderByCreatedAtDesc(childId);

        return reports.stream()
                .map(ReportListResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 리포트 상세 조회
     */
    public ReportResponse getReportDetail(Long reportId) {
        Long centerId = securityUtil.getCurrentCenterId();

        Report report = reportRepository.findByIdWithChild(reportId)
                .orElseThrow(() -> new IllegalArgumentException("리포트를 찾을 수 없습니다."));

        if (!report.getChild().getCenter().getId().equals(centerId)) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }

        return ReportResponse.from(report);
    }

    /**
     * 리포트 삭제
     * - 같은 센터 소속의 Admin/Therapist만 삭제 가능
     */
    @Transactional
    public void deleteReport(Long reportId) {
        Long centerId = securityUtil.getCurrentCenterId();

        Report report = reportRepository.findByIdWithChild(reportId)
                .orElseThrow(() -> new IllegalArgumentException("리포트를 찾을 수 없습니다."));

        // 권한 검증: 같은 센터 소속인지
        if (!report.getChild().getCenter().getId().equals(centerId)) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }

        reportRepository.delete(report);
        log.info("리포트 삭제 완료 - ID: {}, 제목: {}", reportId, report.getTitle());
    }

    /**
     * 통계 계산
     */
    private StatisticsResult calculateStatistics(List<Session> sessions) {
        int totalSessions = sessions.size();
        int totalTrials = 0;
        int totalSuccesses = 0;

        for (Session session : sessions) {
            for (SessionTrial trial : session.getTrials()) {
                totalTrials += trial.getTrials();
                totalSuccesses += trial.getSuccesses();
            }
        }

        BigDecimal averageAccuracy = null;
        if (totalTrials > 0) {
            averageAccuracy = BigDecimal.valueOf(totalSuccesses)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalTrials), 2, RoundingMode.HALF_UP);
        }

        return new StatisticsResult(totalSessions, totalTrials, totalSuccesses, averageAccuracy);
    }

    /**
     * 목표별 상세 통계 계산
     */
    private List<GoalDetail> calculateGoalDetails(Long childId, List<Session> sessions) {
        // 아동의 모든 목표 조회
        List<Goal> goals = goalRepository.findByChildId(childId);
        if (goals.isEmpty()) {
            return List.of();
        }

        // 목표별 시행 데이터 집계
        Map<Long, GoalStats> goalStatsMap = new HashMap<>();
        for (Goal goal : goals) {
            goalStatsMap.put(goal.getId(), new GoalStats(goal));
        }

        // 세션 시행 데이터 집계
        for (Session session : sessions) {
            for (SessionTrial trial : session.getTrials()) {
                Long goalId = trial.getGoal().getId();
                GoalStats stats = goalStatsMap.get(goalId);
                if (stats != null) {
                    stats.addTrial(trial);
                }
            }
        }

        // GoalDetail 리스트 생성
        return goalStatsMap.values().stream()
                .filter(stats -> stats.totalTrials > 0) // 시행 기록이 있는 목표만
                .map(GoalStats::toGoalDetail)
                .sorted((a, b) -> Integer.compare(b.getTotalTrials(), a.getTotalTrials())) // 시행 횟수 많은 순
                .collect(Collectors.toList());
    }

    /**
     * 리포트 내용 생성 (타입별 분기)
     */
    private String generateReportContent(
            ReportType reportType,
            Child child,
            LocalDate periodStart,
            LocalDate periodEnd,
            StatisticsResult stats,
            List<GoalDetail> goalDetails,
            String sessionNotes,
            List<Session> sessions  // 세션 목록 추가
    ) {
        // PARENT_SUMMARY인 경우 구조화된 JSON 리포트 생성
        if (reportType == ReportType.PARENT_SUMMARY) {
            log.info("PARENT_SUMMARY 타입 - 구조화된 AI 리포트 생성 시작");

            // 날짜별 세션 점수 계산 (최근 10개)
            List<SessionScore> sessionScores = calculateSessionScores(sessions);

            StructuredReportContext context = StructuredReportContext.builder()
                    .childName(child.getName())
                    .periodStart(periodStart)
                    .periodEnd(periodEnd)
                    .totalSessions(stats.totalSessions)
                    .totalTrials(stats.totalTrials)
                    .totalSuccesses(stats.totalSuccesses)
                    .averageAccuracy(stats.averageAccuracy)
                    .sessionNotes(sessionNotes)
                    .goalDetails(goalDetails)
                    .sessionScores(sessionScores)
                    .build();

            AiReportContent aiContent = openAiService.generateStructuredReport(context);

            // JSON으로 직렬화하여 content에 저장
            try {
                return objectMapper.writeValueAsString(aiContent);
            } catch (JsonProcessingException e) {
                log.error("AI 리포트 JSON 직렬화 실패", e);
                // 폴백: 통계 코멘트만 반환
                return generateStatisticsComment(periodStart, periodEnd, stats, child.getName(), goalDetails);
            }
        }

        // 다른 타입은 통계만 반환
        return generateStatisticsComment(periodStart, periodEnd, stats, child.getName(), goalDetails);
    }

    /**
     * 날짜별 세션 평균 점수 계산 (차트용)
     * 최근 10개 세션까지만 반환
     */
    private List<SessionScore> calculateSessionScores(List<Session> sessions) {
        if (sessions == null || sessions.isEmpty()) {
            return List.of();
        }

        // 날짜별 시행/성공 집계
        Map<LocalDate, int[]> dateStats = new TreeMap<>(); // TreeMap으로 날짜순 정렬

        for (Session session : sessions) {
            LocalDate date = session.getSessionDate();
            int[] stats = dateStats.computeIfAbsent(date, k -> new int[2]); // [trials, successes]

            for (SessionTrial trial : session.getTrials()) {
                stats[0] += trial.getTrials();
                stats[1] += trial.getSuccesses();
            }
        }

        // SessionScore 리스트 생성 (최근 10개)
        return dateStats.entrySet().stream()
                .map(entry -> {
                    int trials = entry.getValue()[0];
                    int successes = entry.getValue()[1];
                    BigDecimal score = trials > 0
                            ? BigDecimal.valueOf(successes)
                                .multiply(BigDecimal.valueOf(100))
                                .divide(BigDecimal.valueOf(trials), 1, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;

                    return SessionScore.builder()
                            .date(entry.getKey())
                            .score(score)
                            .build();
                })
                .sorted(Comparator.comparing(SessionScore::getDate))
                .limit(10)
                .toList();
    }

    /**
     * 통계 코멘트 생성
     */
    private String generateStatisticsComment(
            LocalDate periodStart,
            LocalDate periodEnd,
            StatisticsResult stats,
            String childName,
            List<GoalDetail> goalDetails
    ) {
        StringBuilder sb = new StringBuilder();

        String startStr = periodStart.format(DATE_FORMATTER);
        String endStr = periodEnd.format(DATE_FORMATTER);

        sb.append(String.format("📊 %s ~ %s 기간 리포트\n\n", startStr, endStr));

        if (stats.totalSessions == 0) {
            sb.append("해당 기간에 진행된 세션이 없습니다.");
            return sb.toString();
        }

        sb.append(String.format("[%s 아동 치료 요약]\n", childName));
        sb.append(String.format("• 총 세션 수: %d회\n", stats.totalSessions));
        sb.append(String.format("• 총 시행 횟수: %d회\n", stats.totalTrials));
        sb.append(String.format("• 총 성공 횟수: %d회\n", stats.totalSuccesses));

        if (stats.averageAccuracy != null) {
            sb.append(String.format("• 평균 정반응률: %s%%\n", stats.averageAccuracy.toPlainString()));

            // 수행 정확도에 따른 간단 평가
            String evaluation = getAccuracyEvaluation(stats.averageAccuracy);
            sb.append(String.format("\n💡 평가: %s\n", evaluation));
        }

        // 목표별 요약
        if (!goalDetails.isEmpty()) {
            sb.append("\n[목표별 달성 현황]\n");
            for (GoalDetail goal : goalDetails) {
                String statusIcon = getGoalStatusIcon(goal);
                sb.append(String.format("%s %s: %s%% (목표 %d%%) - %s\n",
                        statusIcon,
                        goal.getGoalName(),
                        goal.getActualSuccessRate() != null ? goal.getActualSuccessRate().toPlainString() : "N/A",
                        goal.getTargetSuccessRate(),
                        getCategoryKorean(goal.getCategory())
                ));
            }
        }

        return sb.toString();
    }

    private String getAccuracyEvaluation(BigDecimal accuracy) {
        if (accuracy.compareTo(BigDecimal.valueOf(80)) >= 0) {
            return "우수한 수행 정확도입니다. 목표 상향 조정을 고려해볼 수 있습니다.";
        } else if (accuracy.compareTo(BigDecimal.valueOf(60)) >= 0) {
            return "양호한 수행 정확도입니다. 지속적인 연습으로 향상이 기대됩니다.";
        } else if (accuracy.compareTo(BigDecimal.valueOf(40)) >= 0) {
            return "보통 수준입니다. 촉구 수준 조정 또는 목표 세분화를 고려해볼 수 있습니다.";
        } else {
            return "수행 정확도 향상이 필요합니다. 목표 난이도 조정을 권장합니다.";
        }
    }

    private String getGoalStatusIcon(GoalDetail goal) {
        if (goal.getActualSuccessRate() == null) return "⚪";
        if (goal.getActualSuccessRate().compareTo(BigDecimal.valueOf(goal.getTargetSuccessRate())) >= 0) {
            return "✅"; // 목표 달성
        } else if (goal.getActualSuccessRate().compareTo(BigDecimal.valueOf(goal.getTargetSuccessRate() * 0.8)) >= 0) {
            return "🔵"; // 목표 근접
        } else {
            return "🔶"; // 진행 중
        }
    }

    private String getCategoryKorean(String category) {
        return switch (category) {
            case "COMMUNICATION" -> "의사소통";
            case "SOCIAL" -> "사회성";
            case "SENSORY" -> "감각통합";
            case "SELF_CARE" -> "신변처리";
            case "COGNITIVE" -> "인지";
            case "MOTOR" -> "운동";
            case "PLAY" -> "놀이";
            case "BEHAVIOR" -> "행동";
            default -> category;
        };
    }

    /**
     * 통계 결과 내부 클래스
     */
    private record StatisticsResult(
            int totalSessions,
            int totalTrials,
            int totalSuccesses,
            BigDecimal averageAccuracy
    ) {}

    /**
     * 목표별 통계 집계용 내부 클래스
     */
    private static class GoalStats {
        private final Goal goal;
        private int totalTrials = 0;
        private int totalSuccesses = 0;
        private int totalPromptCount = 0;

        GoalStats(Goal goal) {
            this.goal = goal;
        }

        void addTrial(SessionTrial trial) {
            totalTrials += trial.getTrials();
            totalSuccesses += trial.getSuccesses();
            if (trial.getPromptCount() != null) {
                totalPromptCount += trial.getPromptCount();
            }
        }

        GoalDetail toGoalDetail() {
            BigDecimal actualSuccessRate = null;
            if (totalTrials > 0) {
                actualSuccessRate = BigDecimal.valueOf(totalSuccesses)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalTrials), 2, RoundingMode.HALF_UP);
            }

            return GoalDetail.builder()
                    .goalName(goal.getName())
                    .category(goal.getCategory().name())
                    .targetSuccessRate(goal.getTargetSuccessRate() != null ? goal.getTargetSuccessRate() : 80)
                    .actualSuccessRate(actualSuccessRate)
                    .totalTrials(totalTrials)
                    .totalSuccesses(totalSuccesses)
                    .primaryPromptType(String.valueOf(totalPromptCount))
                    .build();
        }
    }
}
