package com.aba.os.abaosserver.dto.migration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 이미지 마이그레이션 결과 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageMigrationResponse {

    private boolean success;
    private String message;

    // 처리 결과
    private Long childId;
    private String childName;
    private int goalsCreated;

    // AI 추출 원본 데이터 (디버깅/확인용)
    private ExtractedChildData extractedData;

    // 오류 목록
    private List<String> errors;

    public static ImageMigrationResponse success(Long childId, String childName, int goalsCreated, ExtractedChildData extractedData) {
        return ImageMigrationResponse.builder()
                .success(true)
                .message(String.format("이미지에서 아동 '%s' 정보와 %d개의 목표를 성공적으로 추출하여 저장했습니다.",
                        childName, goalsCreated))
                .childId(childId)
                .childName(childName)
                .goalsCreated(goalsCreated)
                .extractedData(extractedData)
                .build();
    }

    public static ImageMigrationResponse error(String message, List<String> errors) {
        return ImageMigrationResponse.builder()
                .success(false)
                .message(message)
                .errors(errors)
                .build();
    }

    public static ImageMigrationResponse partialSuccess(Long childId, String childName,
                                                         int goalsCreated, List<String> errors,
                                                         ExtractedChildData extractedData) {
        return ImageMigrationResponse.builder()
                .success(true)
                .message(String.format("아동 '%s' 저장 완료. 목표 %d개 생성. 일부 오류 발생.",
                        childName, goalsCreated))
                .childId(childId)
                .childName(childName)
                .goalsCreated(goalsCreated)
                .extractedData(extractedData)
                .errors(errors)
                .build();
    }
}
