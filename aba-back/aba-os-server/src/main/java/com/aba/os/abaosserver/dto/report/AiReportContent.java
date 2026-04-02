package com.aba.os.abaosserver.dto.report;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * AI가 생성한 리포트 구조화된 응답 DTO
 * 프론트엔드에서 차트 및 텍스트 표시에 사용
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI 생성 리포트 구조화 데이터")
public class AiReportContent {

    @Schema(description = "전반적인 아동의 발달 상태 및 세션 수행 요약 (3~4문장)",
            example = "김철수 아동은 이번 기간 동안 눈에 띄는 발전을 보여주었습니다...")
    private String summary;

    @JsonProperty("strength_weakness")
    @Schema(description = "주요 강점과 보완이 필요한 부분 분석",
            example = "강점: 언어적 촉구에 대한 반응이 빠르고 적극적입니다...")
    private String strengthWeakness;

    @Schema(description = "가정 연계 활동 및 다음 치료 방향 제안",
            example = "가정에서는 식사 시간을 활용하여 눈 맞춤 연습을 해주세요...")
    private String recommendation;

    @JsonProperty("chart_data")
    @Schema(description = "날짜별 수행 정확도 데이터 (차트용)")
    private List<ChartDataPoint> chartData;

    /**
     * 차트 데이터 포인트
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "차트용 날짜-점수 데이터 포인트")
    public static class ChartDataPoint {

        @Schema(description = "세션 날짜", example = "2026-02-01")
        private String date;

        @Schema(description = "해당 날짜의 평균 수행 정확도 (%)", example = "75.5")
        private BigDecimal score;
    }
}
