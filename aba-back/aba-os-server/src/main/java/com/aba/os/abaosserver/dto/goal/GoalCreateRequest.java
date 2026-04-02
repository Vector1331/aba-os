package com.aba.os.abaosserver.dto.goal;

import com.aba.os.abaosserver.domain.Goal.GoalCategory;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class GoalCreateRequest {

    @NotBlank(message = "목표명은 필수입니다")
    private String name;

    @NotNull(message = "카테고리는 필수입니다")
    private GoalCategory category;

    private String description;

    @Min(value = 0, message = "목표 성공률은 0 이상이어야 합니다")
    @Max(value = 100, message = "목표 성공률은 100 이하여야 합니다")
    private Integer targetSuccessRate;

    @Min(value = 1, message = "연속 달성일은 1 이상이어야 합니다")
    private Integer consecutiveDays;

    private String promptPlan;
}
