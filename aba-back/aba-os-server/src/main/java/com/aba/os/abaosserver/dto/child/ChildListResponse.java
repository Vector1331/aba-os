package com.aba.os.abaosserver.dto.child;

import com.aba.os.abaosserver.domain.Child;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
public class ChildListResponse {

    private UUID id;
    private String name;
    private LocalDate birthDate;
    private String gender;
    private String diagnosis;
    private String therapistName;
    private String status;

    public static ChildListResponse from(Child child) {
        return ChildListResponse.builder()
                .id(child.getId())
                .name(child.getName())
                .birthDate(child.getBirthDate())
                .gender(child.getGender().name())
                .diagnosis(child.getDiagnosis())
                .therapistName(child.getTherapist().getUser().getName())
                .status(child.getStatus())
                .build();
    }
}
