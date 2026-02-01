package com.aba.os.abaosserver.dto.report;

import com.aba.os.abaosserver.domain.Report;
import com.aba.os.abaosserver.domain.Report.ReportStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ReportListResponse {

    private UUID id;
    private String title;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private Integer totalSessions;
    private BigDecimal averageAccuracy;
    private ReportStatus status;
    private LocalDateTime createdAt;

    public static ReportListResponse from(Report report) {
        return ReportListResponse.builder()
                .id(report.getId())
                .title(report.getTitle())
                .periodStart(report.getPeriodStart())
                .periodEnd(report.getPeriodEnd())
                .totalSessions(report.getTotalSessions())
                .averageAccuracy(report.getAverageAccuracy())
                .status(report.getStatus())
                .createdAt(report.getCreatedAt())
                .build();
    }
}
