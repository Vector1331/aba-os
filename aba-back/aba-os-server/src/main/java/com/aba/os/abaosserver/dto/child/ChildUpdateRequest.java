package com.aba.os.abaosserver.dto.child;

import com.aba.os.abaosserver.domain.Child.Gender;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class ChildUpdateRequest {
    private String name;
    private LocalDate birthDate;
    private Gender gender;
    private String diagnosis;
    private String currentDevLevel;
    private String parentCharacteristics;
    private String requestDetails;
    private String status;
}
