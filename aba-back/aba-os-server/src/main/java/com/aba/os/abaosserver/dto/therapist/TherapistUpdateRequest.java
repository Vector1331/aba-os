package com.aba.os.abaosserver.dto.therapist;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TherapistUpdateRequest {

    private String specialty;

    private Integer experienceYears;
}
