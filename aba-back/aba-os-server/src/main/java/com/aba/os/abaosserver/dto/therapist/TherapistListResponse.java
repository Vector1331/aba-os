package com.aba.os.abaosserver.dto.therapist;

import com.aba.os.abaosserver.domain.Therapist;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TherapistListResponse {

    private Long id;
    private String name;
    private String email;
    private String specialty;
    private Integer experienceYears;

    public static TherapistListResponse from(Therapist therapist) {
        return TherapistListResponse.builder()
                .id(therapist.getId())
                .name(therapist.getUser().getName())
                .email(therapist.getUser().getEmail())
                .specialty(therapist.getSpecialty())
                .experienceYears(therapist.getExperienceYears())
                .build();
    }
}
