package com.aba.os.abaosserver.dto.dashboard;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class DashboardSummaryResponse {

    private Long activeCasesCount;           // 활성 케이스 수 (활성 아동 수)
    private Long weeklySessionsCount;        // 주간 세션 수 (이번 주)
    private Map<String, Long> developmentAgeStats;  // 발달 연령 현황 (연령대별 아동 수)
    private Long pendingReportsCount;        // 미작성 리포트 수
    private List<RecentSessionDto> recentSessions;  // 최근 세션 Top 5

    @Getter
    @Builder
    public static class RecentSessionDto {
        private String sessionId;
        private String childId;
        private String childName;
        private String therapistName;
        private String sessionDate;
        private BigDecimal successRate;      // 성공률 (%)
        private Integer totalTrials;         // 총 시행 수
        private Integer totalSuccesses;      // 총 성공 수
    }

    // 하위 호환성을 위한 기존 필드 (deprecated)
    @Deprecated
    private Long totalActiveChildren;
    @Deprecated
    private Long sessionsToday;
    @Deprecated
    private Long totalSessionsThisMonth;
    @Deprecated
    private List<UpcomingSessionDto> upcomingSessions;

    @Getter
    @Builder
    @Deprecated
    public static class UpcomingSessionDto {
        private String sessionId;
        private String childId;
        private String childName;
        private String therapistName;
        private String sessionDate;
        private Integer duration;
    }
}
