package com.aba.os.abaosserver.service;

import com.aba.os.abaosserver.domain.Child;
import com.aba.os.abaosserver.domain.Report;
import com.aba.os.abaosserver.domain.Session;
import com.aba.os.abaosserver.domain.SessionTrial;
import com.aba.os.abaosserver.dto.report.ReportCreateRequest;
import com.aba.os.abaosserver.dto.report.ReportListResponse;
import com.aba.os.abaosserver.dto.report.ReportResponse;
import com.aba.os.abaosserver.repository.ChildRepository;
import com.aba.os.abaosserver.repository.ReportRepository;
import com.aba.os.abaosserver.repository.SessionRepository;
import com.aba.os.abaosserver.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepository;
    private final SessionRepository sessionRepository;
    private final ChildRepository childRepository;
    private final SecurityUtil securityUtil;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 리포트 생성
     * - 기간 내 세션 데이터 집계
     * - 통계 계산 및 자동 코멘트 생성
     */
    @Transactional
    public ReportResponse createReport(ReportCreateRequest request) {
        UUID centerId = securityUtil.getCurrentCenterId();

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

        // 통계 집계
        int totalSessions = sessions.size();
        int totalTrials = 0;
        int totalSuccesses = 0;

        for (Session session : sessions) {
            for (SessionTrial trial : session.getTrials()) {
                totalTrials += trial.getTrials();
                totalSuccesses += trial.getSuccesses();
            }
        }

        // 평균 수행 정확도 계산
        BigDecimal averageAccuracy = null;
        if (totalTrials > 0) {
            averageAccuracy = BigDecimal.valueOf(totalSuccesses)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalTrials), 2, RoundingMode.HALF_UP);
        }

        // 자동 코멘트 생성
        String content = generateAutoComment(
                request.getPeriodStart(),
                request.getPeriodEnd(),
                totalSessions,
                totalTrials,
                totalSuccesses,
                averageAccuracy,
                child.getName()
        );

        // Report 엔티티 생성 및 저장
        Report report = Report.builder()
                .child(child)
                .title(request.getTitle())
                .periodStart(request.getPeriodStart())
                .periodEnd(request.getPeriodEnd())
                .totalSessions(totalSessions)
                .totalTrials(totalTrials)
                .totalSuccesses(totalSuccesses)
                .averageAccuracy(averageAccuracy)
                .content(content)
                .build();

        Report savedReport = reportRepository.save(report);

        return ReportResponse.from(savedReport);
    }

    /**
     * 아동별 리포트 목록 조회
     */
    public List<ReportListResponse> getReports(UUID childId) {
        UUID centerId = securityUtil.getCurrentCenterId();

        // 아동 조회 및 검증
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
    public ReportResponse getReportDetail(UUID reportId) {
        UUID centerId = securityUtil.getCurrentCenterId();

        Report report = reportRepository.findByIdWithChild(reportId)
                .orElseThrow(() -> new IllegalArgumentException("리포트를 찾을 수 없습니다."));

        // 같은 센터 소속인지 검증
        if (!report.getChild().getCenter().getId().equals(centerId)) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }

        return ReportResponse.from(report);
    }

    /**
     * 자동 코멘트 생성
     */
    private String generateAutoComment(
            LocalDate periodStart,
            LocalDate periodEnd,
            int totalSessions,
            int totalTrials,
            int totalSuccesses,
            BigDecimal averageAccuracy,
            String childName
    ) {
        StringBuilder sb = new StringBuilder();

        String startStr = periodStart.format(DATE_FORMATTER);
        String endStr = periodEnd.format(DATE_FORMATTER);

        sb.append(String.format("%s ~ %s 기간 동안 총 %d회의 세션이 진행되었습니다.\n\n",
                startStr, endStr, totalSessions));

        if (totalSessions == 0) {
            sb.append("해당 기간에 진행된 세션이 없습니다.");
            return sb.toString();
        }

        sb.append(String.format("[%s 아동 치료 요약]\n", childName));
        sb.append(String.format("- 총 시행 횟수: %d회\n", totalTrials));
        sb.append(String.format("- 총 성공 횟수: %d회\n", totalSuccesses));

        if (averageAccuracy != null) {
            sb.append(String.format("- 평균 수행 정확도: %s%%\n", averageAccuracy.toPlainString()));

            // 수행 정확도에 따른 코멘트 추가
            if (averageAccuracy.compareTo(BigDecimal.valueOf(80)) >= 0) {
                sb.append("\n우수한 수행 정확도를 보이고 있습니다. 현재 목표 유지 또는 상향 조정을 고려해 볼 수 있습니다.");
            } else if (averageAccuracy.compareTo(BigDecimal.valueOf(60)) >= 0) {
                sb.append("\n양호한 수행 정확도를 보이고 있습니다. 지속적인 연습으로 향상이 기대됩니다.");
            } else if (averageAccuracy.compareTo(BigDecimal.valueOf(40)) >= 0) {
                sb.append("\n보통 수준의 수행 정확도입니다. 촉구 수준 조정 또는 목표 세분화를 고려해 볼 수 있습니다.");
            } else {
                sb.append("\n수행 정확도 향상이 필요합니다. 목표 난이도 조정 또는 촉구 강화를 권장합니다.");
            }
        }

        return sb.toString();
    }
}
