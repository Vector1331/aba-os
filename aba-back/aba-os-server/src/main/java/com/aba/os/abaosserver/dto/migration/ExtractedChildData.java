package com.aba.os.abaosserver.dto.migration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Vision AI가 이미지에서 추출한 아동 데이터 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractedChildData {

    private String childName;
    private String birthDate;       // yyyy-MM-dd 형식
    private String gender;          // M/F 또는 MALE/FEMALE
    private String diagnosis;
    private List<ExtractedGoal> goals;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExtractedGoal {
        private String title;
        private String category;    // SOCIAL, BEHAVIOR, COMMUNICATION 등
        private String status;      // IN_PROGRESS, WAITING, COMPLETED
        private String description;
    }
}
