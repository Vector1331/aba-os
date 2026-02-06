package com.aba.os.abaosserver.dto.child;

import com.aba.os.abaosserver.domain.Child.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class ChildCreateRequest {

    @NotBlank(message = "아동 이름은 필수입니다")
    private String name;

    @NotNull(message = "생년월일은 필수입니다")
    private LocalDate birthDate;

    @NotNull(message = "성별은 필수입니다")
    private Gender gender;

    private String diagnosis;

    private String currentDevLevel;

    private String parentCharacteristics;

    private String requestDetails;

    @NotNull(message = "담당 치료사 ID는 필수입니다")
    private Long therapistId;
}
