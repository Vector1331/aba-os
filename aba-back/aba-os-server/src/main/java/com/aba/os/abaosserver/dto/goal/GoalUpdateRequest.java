package com.aba.os.abaosserver.dto.goal;

import com.aba.os.abaosserver.domain.Goal.GoalCategory;
import com.aba.os.abaosserver.domain.Goal.GoalStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class GoalUpdateRequest {
    private String name;
    private GoalCategory category;
    private String description;
    private GoalStatus status;
    private Integer targetSuccessRate;
    private Integer consecutiveDays;
    private String promptPlan;
}
