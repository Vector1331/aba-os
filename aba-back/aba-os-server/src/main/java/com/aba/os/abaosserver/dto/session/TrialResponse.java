package com.aba.os.abaosserver.dto.session;

import com.aba.os.abaosserver.domain.SessionTrial;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TrialResponse {

    private Long id;
    private Long goalId;
    private String goalName;
    private String taskContent;
    private Integer trials;
    private Integer successes;
    private Double successRate;
    private Integer promptCount; // 촉구 횟수
    private String memo;

    public static TrialResponse from(SessionTrial trial) {
        Double rate = trial.getTrials() > 0
                ? Math.round((double) trial.getSuccesses() / trial.getTrials() * 1000) / 10.0
                : null;

        return TrialResponse.builder()
                .id(trial.getId())
                .goalId(trial.getGoal().getId())
                .goalName(trial.getGoal().getName())
                .taskContent(trial.getTaskContent())
                .trials(trial.getTrials())
                .successes(trial.getSuccesses())
                .successRate(rate)
                .promptCount(trial.getPromptCount())
                .memo(trial.getMemo())
                .build();
    }
}
