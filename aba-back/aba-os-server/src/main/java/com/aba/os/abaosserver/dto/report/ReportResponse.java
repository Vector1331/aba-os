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
public class ReportResponse {

    private UUID id;
    private UUID childId;
    private String childName;
    private String title;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private ReportStats stats;
    private String content;
    private ReportStatus status;
    private LocalDateTime createdAt;

    @Getter
    @Builder
    public static class ReportStats {
        private Integer totalSessions;
        private Integer totalTrials;
        private Integer totalSuccesses;
        private BigDecimal averageAccuracy;
    }

    public static ReportResponse from(Report report) {
        ReportStats stats = ReportStats.builder()
                .totalSessions(report.getTotalSessions())
                .totalTrials(report.getTotalTrials())
                .totalSuccesses(report.getTotalSuccesses())
                .averageAccuracy(report.getAverageAccuracy())
                .build();

        return ReportResponse.builder()
                .id(report.getId())
                .childId(report.getChild().getId())
                .childName(report.getChild().getName())
                .title(report.getTitle())
                .periodStart(report.getPeriodStart())
                .periodEnd(report.getPeriodEnd())
                .stats(stats)
                .content(report.getContent())
                .status(report.getStatus())
                .createdAt(report.getCreatedAt())
                .build();
    }
}
