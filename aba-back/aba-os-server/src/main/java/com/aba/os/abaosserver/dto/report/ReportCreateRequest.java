package com.aba.os.abaosserver.dto.report;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class ReportCreateRequest {

    @NotNull(message = "아동 ID는 필수입니다")
    private UUID childId;

    @NotBlank(message = "리포트 제목은 필수입니다")
    private String title;

    @NotNull(message = "시작 날짜는 필수입니다")
    private LocalDate periodStart;

    @NotNull(message = "종료 날짜는 필수입니다")
    private LocalDate periodEnd;
}
