package com.aba.os.abaosserver.dto.session;

import com.aba.os.abaosserver.domain.SessionTrial.PromptType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
public class TrialRequest {

    @NotNull(message = "목표 ID는 필수입니다")
    private UUID goalId;

    private String taskContent; // 과제 내용

    @NotNull(message = "시행 횟수는 필수입니다")
    @Min(value = 1, message = "시행 횟수는 1 이상이어야 합니다")
    private Integer trials;

    @NotNull(message = "성공 횟수는 필수입니다")
    @Min(value = 0, message = "성공 횟수는 0 이상이어야 합니다")
    private Integer successes;

    @NotNull(message = "촉구 유형은 필수입니다")
    private PromptType promptType;

    private String memo; // 상세 메모
}
