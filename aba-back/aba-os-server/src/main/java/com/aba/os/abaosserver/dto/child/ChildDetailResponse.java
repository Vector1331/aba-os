package com.aba.os.abaosserver.dto.child;

import com.aba.os.abaosserver.domain.Child;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class ChildDetailResponse {

    private ChildInfo info;
    private ChildStats stats;

    @Getter
    @Builder
    public static class ChildInfo {
        private Long id;
        private String name;
        private LocalDate birthDate;
        private String gender;
        private String diagnosis;
        private String currentDevLevel;
        private String parentCharacteristics;
        private String requestDetails;
        private String therapistName;
        private String status;
    }

    @Getter
    @Builder
    public static class ChildStats {
        private long totalSessions;
        private long activeGoals;
        private Double promptRatio;
    }

    public static ChildDetailResponse from(Child child, long totalSessions, long activeGoals, Double promptRatio) {
        ChildInfo info = ChildInfo.builder()
                .id(child.getId())
                .name(child.getName())
                .birthDate(child.getBirthDate())
                .gender(child.getGender().name())
                .diagnosis(child.getDiagnosis())
                .currentDevLevel(child.getCurrentDevLevel())
                .parentCharacteristics(child.getParentCharacteristics())
                .requestDetails(child.getRequestDetails())
                .therapistName(child.getTherapist().getUser().getName())
                .status(child.getStatus())
                .build();

        ChildStats stats = ChildStats.builder()
                .totalSessions(totalSessions)
                .activeGoals(activeGoals)
                .promptRatio(promptRatio)
                .build();

        return ChildDetailResponse.builder()
                .info(info)
                .stats(stats)
                .build();
    }
}
