package com.aba.os.abaosserver.dto.goal;

import com.aba.os.abaosserver.domain.Goal;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class GoalResponse {

    private UUID id;
    private UUID childId;
    private String name;
    private String category;
    private String description;
    private Integer targetSuccessRate;
    private Integer consecutiveDays;
    private String promptPlan;
    private String status;
    private LocalDateTime createdAt;

    public static GoalResponse from(Goal goal) {
        return GoalResponse.builder()
                .id(goal.getId())
                .childId(goal.getChild().getId())
                .name(goal.getName())
                .category(goal.getCategory().name())
                .description(goal.getDescription())
                .targetSuccessRate(goal.getTargetSuccessRate())
                .consecutiveDays(goal.getConsecutiveDays())
                .promptPlan(goal.getPromptPlan())
                .status(goal.getStatus().name())
                .createdAt(goal.getCreatedAt())
                .build();
    }
}
