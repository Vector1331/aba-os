package com.aba.os.abaosserver.dto.report;

import com.aba.os.abaosserver.domain.Report;
import com.aba.os.abaosserver.domain.Report.ReportStatus;
import com.aba.os.abaosserver.domain.Report.ReportType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@Slf4j
@Schema(description = "리포트 상세 응답")
public class ReportResponse {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Schema(description = "리포트 ID", example = "1")
    private Long id;

    @Schema(description = "아동 ID", example = "1")
    private Long childId;

    @Schema(description = "아동 이름", example = "김철수")
    private String childName;

    @Schema(description = "리포트 제목", example = "2026년 2월 발달 리포트")
    private String title;

    @Schema(description = "리포트 기간 시작일", example = "2026-02-01")
    private LocalDate periodStart;

    @Schema(description = "리포트 기간 종료일", example = "2026-02-28")
    private LocalDate periodEnd;

    @Schema(description = "통계 데이터")
    private ReportStats stats;

    @Schema(description = """
            리포트 내용.
            - PARENT_SUMMARY 타입: JSON 문자열 (AiReportContent 구조)
            - 기타 타입: 일반 텍스트

            PARENT_SUMMARY의 경우 JSON.parse()로 파싱하여 사용:
            {
              "summary": "전반적인 발달 상태 요약",
              "strength_weakness": "강점/보완점 분석",
              "recommendation": "가정 연계 활동 제안",
              "chart_data": [{"date": "2026-02-01", "score": 75.5}, ...]
            }
            """)
    private String content;

    @Schema(description = """
            파싱된 AI 리포트 데이터 (PARENT_SUMMARY 타입일 때만 제공).
            프론트엔드에서 바로 사용 가능한 구조화된 객체.
            null이면 content 필드를 직접 파싱해서 사용.
            """)
    private AiReportContent aiContent;

    @Schema(description = "리포트 타입", example = "PARENT_SUMMARY")
    private ReportType reportType;

    @Schema(description = "리포트 상태", example = "GENERATED")
    private ReportStatus status;

    @Schema(description = "생성 일시")
    private LocalDateTime createdAt;

    @Getter
    @Builder
    @Schema(description = "리포트 통계 데이터")
    public static class ReportStats {
        @Schema(description = "총 세션 수", example = "5")
        private Integer totalSessions;

        @Schema(description = "총 시행 횟수", example = "120")
        private Integer totalTrials;

        @Schema(description = "총 성공 횟수", example = "90")
        private Integer totalSuccesses;

        @Schema(description = "평균 수행 정확도 (%)", example = "75.00")
        private BigDecimal averageAccuracy;
    }

    public static ReportResponse from(Report report) {
        ReportStats stats = ReportStats.builder()
                .totalSessions(report.getTotalSessions())
                .totalTrials(report.getTotalTrials())
                .totalSuccesses(report.getTotalSuccesses())
                .averageAccuracy(report.getAverageAccuracy())
                .build();

        // PARENT_SUMMARY 타입이면 content를 AiReportContent로 파싱 시도
        AiReportContent aiContent = null;
        if (report.getReportType() == ReportType.PARENT_SUMMARY && report.getContent() != null) {
            aiContent = parseAiContent(report.getContent());
        }

        return ReportResponse.builder()
                .id(report.getId())
                .childId(report.getChild().getId())
                .childName(report.getChild().getName())
                .title(report.getTitle())
                .periodStart(report.getPeriodStart())
                .periodEnd(report.getPeriodEnd())
                .stats(stats)
                .content(report.getContent())
                .aiContent(aiContent)
                .reportType(report.getReportType())
                .status(report.getStatus())
                .createdAt(report.getCreatedAt())
                .build();
    }

    /**
     * content JSON을 AiReportContent로 파싱
     * 파싱 실패 시 null 반환 (기존 텍스트 형식 리포트 호환)
     */
    private static AiReportContent parseAiContent(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }

        // JSON인지 확인 (간단한 체크)
        String trimmed = content.trim();
        if (!trimmed.startsWith("{")) {
            // JSON이 아니면 파싱 시도하지 않음 (기존 텍스트 형식)
            return null;
        }

        try {
            return objectMapper.readValue(content, AiReportContent.class);
        } catch (JsonProcessingException e) {
            log.debug("AI 리포트 JSON 파싱 실패 (기존 형식일 수 있음): {}", e.getMessage());
            return null;
        }
    }
}
