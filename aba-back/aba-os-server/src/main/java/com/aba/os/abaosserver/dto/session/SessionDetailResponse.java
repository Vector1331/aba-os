package com.aba.os.abaosserver.dto.session;

import com.aba.os.abaosserver.domain.Session;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class SessionDetailResponse {

    private Long id;
    private Long childId;
    private String childName;
    private Long therapistId;
    private String therapistName;
    private LocalDate sessionDate;
    private Integer duration;
    private String notes;
    private LocalDateTime createdAt;
    private List<TrialResponse> trials;
    private SessionStats stats;

    @Getter
    @Builder
    public static class SessionStats {
        private Integer totalTrials;
        private Integer totalSuccesses;
        private Double overallSuccessRate;
        private Integer goalCount;
    }

    public static SessionDetailResponse from(Session session) {
        List<TrialResponse> trialResponses = session.getTrials().stream()
                .map(TrialResponse::from)
                .collect(Collectors.toList());

        int totalTrials = session.getTrials().stream()
                .mapToInt(t -> t.getTrials())
                .sum();
        int totalSuccesses = session.getTrials().stream()
                .mapToInt(t -> t.getSuccesses())
                .sum();

        Double overallRate = totalTrials > 0
                ? Math.round((double) totalSuccesses / totalTrials * 1000) / 10.0
                : null;

        SessionStats stats = SessionStats.builder()
                .totalTrials(totalTrials)
                .totalSuccesses(totalSuccesses)
                .overallSuccessRate(overallRate)
                .goalCount(session.getTrials().size())
                .build();

        return SessionDetailResponse.builder()
                .id(session.getId())
                .childId(session.getChild().getId())
                .childName(session.getChild().getName())
                .therapistId(session.getTherapist().getId())
                .therapistName(session.getTherapist().getUser().getName())
                .sessionDate(session.getSessionDate())
                .duration(session.getDuration())
                .notes(session.getNotes())
                .createdAt(session.getCreatedAt())
                .trials(trialResponses)
                .stats(stats)
                .build();
    }
}
