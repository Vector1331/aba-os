package com.aba.os.abaosserver.dto.dashboard;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class DashboardSummaryResponse {

    private Long totalActiveChildren;       // 활성 아동 수
    private Long sessionsToday;             // 오늘 세션 수
    private Long totalSessionsThisMonth;    // 이번 달 누적 세션 수
    private List<UpcomingSessionDto> upcomingSessions;  // 예정된 세션 목록 (최대 5개)

    @Getter
    @Builder
    public static class UpcomingSessionDto {
        private String sessionId;
        private String childId;
        private String childName;
        private String therapistName;
        private String sessionDate;
        private Integer duration;
    }
}
