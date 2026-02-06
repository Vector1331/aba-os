package com.aba.os.abaosserver.dto.migration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * DIA 템플릿 엑셀 마이그레이션 결과 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiaMigrationResponse {

    private boolean success;
    private String message;

    // 아동 정보
    private Long childId;
    private String childName;
    private boolean childCreated; // true: 신규 생성, false: 기존 아동 사용

    // 세션 정보
    private Long sessionId;
    private LocalDate sessionDate;
    private int duration;

    // 시행 기록 정보
    private int trialsCreated;
    private int totalTrialCount;
    private int totalSuccessCount;
    private int totalPromptCount;

    // 처리된 행 정보
    private int rowsProcessed;
    private int rowsSkipped;

    // 오류 목록
    private List<String> errors;

    public static DiaMigrationResponse success(Long childId, String childName, boolean childCreated,
                                                Long sessionId, LocalDate sessionDate, int duration,
                                                int trialsCreated, int totalTrialCount,
                                                int totalSuccessCount, int totalPromptCount,
                                                int rowsProcessed, int rowsSkipped) {
        return DiaMigrationResponse.builder()
                .success(true)
                .message(String.format("DIA 데이터 마이그레이션 완료: 아동 '%s', 세션 %s, 시행 기록 %d개 생성",
                        childName, sessionDate, trialsCreated))
                .childId(childId)
                .childName(childName)
                .childCreated(childCreated)
                .sessionId(sessionId)
                .sessionDate(sessionDate)
                .duration(duration)
                .trialsCreated(trialsCreated)
                .totalTrialCount(totalTrialCount)
                .totalSuccessCount(totalSuccessCount)
                .totalPromptCount(totalPromptCount)
                .rowsProcessed(rowsProcessed)
                .rowsSkipped(rowsSkipped)
                .build();
    }

    public static DiaMigrationResponse error(String message, List<String> errors) {
        return DiaMigrationResponse.builder()
                .success(false)
                .message(message)
                .errors(errors)
                .build();
    }

    public static DiaMigrationResponse partialSuccess(Long childId, String childName, boolean childCreated,
                                                       Long sessionId, LocalDate sessionDate, int duration,
                                                       int trialsCreated, int totalTrialCount,
                                                       int totalSuccessCount, int totalPromptCount,
                                                       int rowsProcessed, int rowsSkipped,
                                                       List<String> errors) {
        return DiaMigrationResponse.builder()
                .success(true)
                .message(String.format("DIA 데이터 마이그레이션 완료 (일부 오류): 아동 '%s', 시행 기록 %d개 생성",
                        childName, trialsCreated))
                .childId(childId)
                .childName(childName)
                .childCreated(childCreated)
                .sessionId(sessionId)
                .sessionDate(sessionDate)
                .duration(duration)
                .trialsCreated(trialsCreated)
                .totalTrialCount(totalTrialCount)
                .totalSuccessCount(totalSuccessCount)
                .totalPromptCount(totalPromptCount)
                .rowsProcessed(rowsProcessed)
                .rowsSkipped(rowsSkipped)
                .errors(errors)
                .build();
    }
}
