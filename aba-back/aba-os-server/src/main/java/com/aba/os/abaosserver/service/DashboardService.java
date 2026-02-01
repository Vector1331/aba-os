package com.aba.os.abaosserver.service;

import com.aba.os.abaosserver.domain.Session;
import com.aba.os.abaosserver.dto.dashboard.DashboardSummaryResponse;
import com.aba.os.abaosserver.dto.dashboard.DashboardSummaryResponse.UpcomingSessionDto;
import com.aba.os.abaosserver.repository.ChildRepository;
import com.aba.os.abaosserver.repository.SessionRepository;
import com.aba.os.abaosserver.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final ChildRepository childRepository;
    private final SessionRepository sessionRepository;
    private final SecurityUtil securityUtil;

    private static final String ACTIVE_STATUS = "active";

    /**
     * 대시보드 요약 정보 조회
     */
    public DashboardSummaryResponse getDashboardSummary() {
        UUID centerId = securityUtil.getCurrentCenterId();
        LocalDate today = LocalDate.now();

        // 1. 활성 아동 수
        long totalActiveChildren = childRepository.countByCenter_IdAndStatus(centerId, ACTIVE_STATUS);

        // 2. 오늘 세션 수
        long sessionsToday = sessionRepository.countByChild_Center_IdAndSessionDate(centerId, today);

        // 3. 이번 달 누적 세션 수
        YearMonth currentMonth = YearMonth.from(today);
        LocalDate monthStart = currentMonth.atDay(1);
        LocalDate monthEnd = currentMonth.atEndOfMonth();
        long totalSessionsThisMonth = sessionRepository.countByChild_Center_IdAndSessionDateBetween(
                centerId, monthStart, monthEnd);

        // 4. 예정된 세션 목록 (오늘 이후, 최대 5개)
        List<Session> upcomingSessionEntities = sessionRepository
                .findTop5ByChild_Center_IdAndSessionDateGreaterThanEqualOrderBySessionDateAsc(centerId, today);

        List<UpcomingSessionDto> upcomingSessions = upcomingSessionEntities.stream()
                .map(this::toUpcomingSessionDto)
                .collect(Collectors.toList());

        return DashboardSummaryResponse.builder()
                .totalActiveChildren(totalActiveChildren)
                .sessionsToday(sessionsToday)
                .totalSessionsThisMonth(totalSessionsThisMonth)
                .upcomingSessions(upcomingSessions)
                .build();
    }

    private UpcomingSessionDto toUpcomingSessionDto(Session session) {
        return UpcomingSessionDto.builder()
                .sessionId(session.getId().toString())
                .childId(session.getChild().getId().toString())
                .childName(session.getChild().getName())
                .therapistName(session.getTherapist().getUser().getName())
                .sessionDate(session.getSessionDate().toString())
                .duration(session.getDuration())
                .build();
    }
}
