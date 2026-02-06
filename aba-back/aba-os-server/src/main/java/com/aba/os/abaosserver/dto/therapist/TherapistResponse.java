package com.aba.os.abaosserver.dto.therapist;

import com.aba.os.abaosserver.domain.Therapist;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TherapistResponse {

    private Long id;
    private Long userId;
    private String name;
    private String email;
    private String specialty;
    private Integer experienceYears;
    private boolean deleted;

    public static TherapistResponse from(Therapist therapist) {
        return TherapistResponse.builder()
                .id(therapist.getId())
                .userId(therapist.getUser().getId())
                .name(therapist.getUser().getName())
                .email(therapist.getUser().getEmail())
                .specialty(therapist.getSpecialty())
                .experienceYears(therapist.getExperienceYears())
                .deleted(therapist.isDeleted())
                .build();
    }
}
