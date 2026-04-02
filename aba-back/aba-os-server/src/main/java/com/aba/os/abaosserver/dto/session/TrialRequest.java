package com.aba.os.abaosserver.dto.session;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TrialRequest {

    @NotNull(message = "목표 ID는 필수입니다")
    private Long goalId;

    private String taskContent; // 과제 내용

    @NotNull(message = "시행 횟수는 필수입니다")
    @Min(value = 1, message = "시행 횟수는 1 이상이어야 합니다")
    private Integer trials;

    @NotNull(message = "성공 횟수는 필수입니다")
    @Min(value = 0, message = "성공 횟수는 0 이상이어야 합니다")
    private Integer successes;

    @Min(value = 0, message = "촉구 횟수는 0 이상이어야 합니다")
    private Integer promptCount; // 촉구 횟수

    private String memo; // 상세 메모
}
