package com.aba.os.abaosserver.dto.session;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Getter
@NoArgsConstructor
public class SessionCreateRequest {

    @NotNull(message = "아동 ID는 필수입니다")
    private Long childId;

    @NotNull(message = "치료사 ID는 필수입니다")
    private Long therapistId;

    @NotNull(message = "세션 날짜는 필수입니다")
    private LocalDate sessionDate;

    @NotNull(message = "소요 시간은 필수입니다")
    @Min(value = 1, message = "소요 시간은 1분 이상이어야 합니다")
    private Integer duration;

    private String notes; // 특이사항

    @NotEmpty(message = "시행 기록은 최소 1개 이상 필요합니다")
    @Valid
    private List<TrialRequest> trials;
}
