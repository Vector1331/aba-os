package com.aba.os.abaosserver.service;

import com.aba.os.abaosserver.domain.Child;
import com.aba.os.abaosserver.domain.Session;
import com.aba.os.abaosserver.domain.SessionTrial;
import com.aba.os.abaosserver.dto.dashboard.DashboardSummaryResponse;
import com.aba.os.abaosserver.dto.dashboard.DashboardSummaryResponse.RecentSessionDto;
import com.aba.os.abaosserver.repository.ChildRepository;
import com.aba.os.abaosserver.repository.ReportRepository;
import com.aba.os.abaosserver.repository.SessionRepository;
import com.aba.os.abaosserver.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final ChildRepository childRepository;
    private final SessionRepository sessionRepository;
    private final ReportRepository reportRepository;
    private final SecurityUtil securityUtil;

    private static final String ACTIVE_STATUS = "active";
    private static final int RECENT_SESSIONS_LIMIT = 5;

    /**
     * 대시보드 요약 정보 조회
     */
    public DashboardSummaryResponse getDashboardSummary() {
        Long centerId = securityUtil.getCurrentCenterId();
        LocalDate today = LocalDate.now();

        // 1. 활성 케이스 수 (활성 아동 수)
        long activeCasesCount = childRepository.countByCenter_IdAndStatus(centerId, ACTIVE_STATUS);

        // 2. 주간 세션 수 (이번 주 월요일 ~ 일요일)
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        long weeklySessionsCount = sessionRepository.countByChild_Center_IdAndSessionDateBetween(
                centerId, weekStart, weekEnd);

        // 3. 발달 연령 현황 (연령대별 아동 수)
        Map<String, Long> developmentAgeStats = calculateDevelopmentAgeStats(centerId, today);

        // 4. 미작성 리포트 수 (최근 30일 내 세션이 있지만 리포트가 없는 아동 수)
        long pendingReportsCount = calculatePendingReportsCount(centerId, today);

        // 5. 최근 세션 Top 5 (성공률 포함)
        List<RecentSessionDto> recentSessions = getRecentSessions(centerId);

        log.debug("Dashboard summary - centerId: {}, activeCases: {}, weeklySessions: {}, pendingReports: {}",
                centerId, activeCasesCount, weeklySessionsCount, pendingReportsCount);

        return DashboardSummaryResponse.builder()
                .activeCasesCount(activeCasesCount)
                .weeklySessionsCount(weeklySessionsCount)
                .developmentAgeStats(developmentAgeStats)
                .pendingReportsCount(pendingReportsCount)
                .recentSessions(recentSessions)
                // 하위 호환성을 위한 기존 필드 (deprecated)
                .totalActiveChildren(activeCasesCount)
                .sessionsToday(sessionRepository.countByChild_Center_IdAndSessionDate(centerId, today))
                .totalSessionsThisMonth(calculateMonthlySessionCount(centerId, today))
                .build();
    }

    /**
     * 발달 연령 현황 계산 (연령대별 아동 수)
     */
    private Map<String, Long> calculateDevelopmentAgeStats(Long centerId, LocalDate today) {
        List<Child> activeChildren = childRepository.findByCenter_IdAndStatus(centerId, ACTIVE_STATUS);

        // 연령대별 카운트 초기화 (순서 유지를 위해 LinkedHashMap 사용)
        Map<String, Long> ageStats = new LinkedHashMap<>();
        ageStats.put("0-3세", 0L);
        ageStats.put("4-6세", 0L);
        ageStats.put("7-9세", 0L);
        ageStats.put("10세 이상", 0L);

        for (Child child : activeChildren) {
            int age = calculateAge(child.getBirthDate(), today);
            String ageGroup = getAgeGroup(age);
            ageStats.merge(ageGroup, 1L, Long::sum);
        }

        return ageStats;
    }

    /**
     * 나이 계산
     */
    private int calculateAge(LocalDate birthDate, LocalDate today) {
        if (birthDate == null) {
            return 0;
        }
        return Period.between(birthDate, today).getYears();
    }

    /**
     * 연령대 그룹 반환
     */
    private String getAgeGroup(int age) {
        if (age <= 3) {
            return "0-3세";
        } else if (age <= 6) {
            return "4-6세";
        } else if (age <= 9) {
            return "7-9세";
        } else {
            return "10세 이상";
        }
    }

    /**
     * 미작성 리포트 수 계산
     * 활성 아동 중 최근 30일 이내에 리포트가 없는 아동 수
     */
    private long calculatePendingReportsCount(Long centerId, LocalDate today) {
        // 활성 아동 수
        long activeChildrenCount = childRepository.countByCenter_IdAndStatus(centerId, ACTIVE_STATUS);

        // 최근 30일 이내 리포트가 있는 아동 ID 목록
        LocalDate periodStart = today.minusDays(30);
        List<Long> childIdsWithReport = reportRepository.findChildIdsWithReportInPeriod(centerId, periodStart);

        // 활성 아동 중 리포트가 없는 아동 수
        return activeChildrenCount - childIdsWithReport.size();
    }

    /**
     * 최근 세션 Top 5 조회 (성공률 포함)
     */
    private List<RecentSessionDto> getRecentSessions(Long centerId) {
        List<Session> sessions = sessionRepository.findRecentSessionsWithTrials(centerId);

        return sessions.stream()
                .limit(RECENT_SESSIONS_LIMIT)
                .map(this::toRecentSessionDto)
                .collect(Collectors.toList());
    }

    /**
     * Session -> RecentSessionDto 변환
     */
    private RecentSessionDto toRecentSessionDto(Session session) {
        // 시행 데이터에서 성공률 계산
        List<SessionTrial> trials = session.getTrials();
        int totalTrials = 0;
        int totalSuccesses = 0;

        if (trials != null && !trials.isEmpty()) {
            for (SessionTrial trial : trials) {
                totalTrials += trial.getTrials() != null ? trial.getTrials() : 0;
                totalSuccesses += trial.getSuccesses() != null ? trial.getSuccesses() : 0;
            }
        }

        BigDecimal successRate = BigDecimal.ZERO;
        if (totalTrials > 0) {
            successRate = BigDecimal.valueOf(totalSuccesses)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalTrials), 1, RoundingMode.HALF_UP);
        }

        return RecentSessionDto.builder()
                .sessionId(session.getId().toString())
                .childId(session.getChild().getId().toString())
                .childName(session.getChild().getName())
                .therapistName(session.getTherapist().getUser().getName())
                .sessionDate(session.getSessionDate().toString())
                .successRate(successRate)
                .totalTrials(totalTrials)
                .totalSuccesses(totalSuccesses)
                .build();
    }

    /**
     * 이번 달 누적 세션 수 계산 (하위 호환성용)
     */
    private long calculateMonthlySessionCount(Long centerId, LocalDate today) {
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());
        return sessionRepository.countByChild_Center_IdAndSessionDateBetween(centerId, monthStart, monthEnd);
    }
}
