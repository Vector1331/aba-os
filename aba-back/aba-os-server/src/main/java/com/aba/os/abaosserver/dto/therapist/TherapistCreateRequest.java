package com.aba.os.abaosserver.dto.therapist;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TherapistCreateRequest {

    @NotNull(message = "연결할 사용자 ID는 필수입니다")
    private Long userId;

    private String specialty;

    private Integer experienceYears;
}
