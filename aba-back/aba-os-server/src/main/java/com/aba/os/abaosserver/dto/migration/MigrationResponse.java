package com.aba.os.abaosserver.dto.migration;

import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
public class MigrationResponse {

    private int childrenSuccessCount;
    private int childrenSkippedCount;
    private int goalsSuccessCount;
    private int goalsFailureCount;
    private String message;

    @Builder.Default
    private List<String> errors = new ArrayList<>();

    public static MigrationResponse success(int childrenSuccess, int childrenSkipped,
                                            int goalsSuccess, int goalsFailure) {
        String msg = String.format("마이그레이션 완료: 아동 %d명 등록 (%d명 스킵), 목표 %d개 등록 (%d개 실패)",
                childrenSuccess, childrenSkipped, goalsSuccess, goalsFailure);

        return MigrationResponse.builder()
                .childrenSuccessCount(childrenSuccess)
                .childrenSkippedCount(childrenSkipped)
                .goalsSuccessCount(goalsSuccess)
                .goalsFailureCount(goalsFailure)
                .message(msg)
                .build();
    }

    public static MigrationResponse error(String errorMessage, List<String> errors) {
        return MigrationResponse.builder()
                .childrenSuccessCount(0)
                .childrenSkippedCount(0)
                .goalsSuccessCount(0)
                .goalsFailureCount(0)
                .message(errorMessage)
                .errors(errors != null ? errors : new ArrayList<>())
                .build();
    }
}
