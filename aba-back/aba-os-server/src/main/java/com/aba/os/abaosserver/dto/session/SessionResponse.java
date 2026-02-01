package com.aba.os.abaosserver.dto.session;

import com.aba.os.abaosserver.domain.Session;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
public class SessionResponse {

    private UUID id;
    private UUID childId;
    private String childName;
    private UUID therapistId;
    private String therapistName;
    private LocalDate sessionDate;
    private Integer duration;
    private String notes;
    private Integer trialCount; // 시행 기록 수
    private Double successRate; // 평균 성공률

    public static SessionResponse from(Session session) {
        int totalTrials = session.getTrials().stream()
                .mapToInt(t -> t.getTrials())
                .sum();
        int totalSuccesses = session.getTrials().stream()
                .mapToInt(t -> t.getSuccesses())
                .sum();

        Double avgSuccessRate = totalTrials > 0
                ? Math.round((double) totalSuccesses / totalTrials * 1000) / 10.0
                : null;

        return SessionResponse.builder()
                .id(session.getId())
                .childId(session.getChild().getId())
                .childName(session.getChild().getName())
                .therapistId(session.getTherapist().getId())
                .therapistName(session.getTherapist().getUser().getName())
                .sessionDate(session.getSessionDate())
                .duration(session.getDuration())
                .notes(session.getNotes())
                .trialCount(session.getTrials().size())
                .successRate(avgSuccessRate)
                .build();
    }
}
