package com.aba.os.abaosserver.service;

import com.aba.os.abaosserver.dto.migration.ExtractedChildData;
import com.aba.os.abaosserver.dto.report.AiReportContent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class OpenAiService {

    private final WebClient webClient;
    private final String apiKey;
    private final String model;
    private final int maxTokens;
    private final ObjectMapper objectMapper;

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    private static final Duration VISION_TIMEOUT = Duration.ofSeconds(60); // Vision API는 더 긴 타임아웃

    public OpenAiService(
            @Value("${openai.api-key:}") String apiKey,
            @Value("${openai.model:gpt-4o}") String model,
            @Value("${openai.max-tokens:1000}") int maxTokens
    ) {
        this.apiKey = apiKey;
        this.model = model;
        this.maxTokens = maxTokens;
        this.webClient = WebClient.builder()
                .baseUrl(OPENAI_API_URL)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * API 키 유효성 확인
     */
    public boolean isApiKeyConfigured() {
        return StringUtils.hasText(apiKey);
    }

    /**
     * 부모용 리포트 생성 (PARENT_SUMMARY)
     * - 목표별 달성률, 세션 정보를 기반으로 따뜻한 어조의 상담 일지 생성
     */
    public String generateParentSummary(ParentReportContext context) {
        if (!isApiKeyConfigured()) {
            log.info("OpenAI API 키가 설정되지 않았습니다. 더미 데이터를 사용합니다.");
            return generateDummyParentSummary(context);
        }

        try {
            String systemPrompt = buildSystemPrompt();
            String userPrompt = buildParentSummaryPrompt(context);
            return callOpenAiApi(systemPrompt, userPrompt);
        } catch (Exception e) {
            log.warn("OpenAI API 호출 실패. 더미 데이터를 사용합니다. 원인: {}", e.getMessage());
            return generateDummyParentSummary(context);
        }
    }

    /**
     * 기존 호환용 메서드 (단순 통계 기반)
     */
    public String generateReportComment(
            String childName,
            int totalSessions,
            int totalTrials,
            int totalSuccesses,
            BigDecimal averageAccuracy,
            String notes
    ) {
        ParentReportContext context = ParentReportContext.builder()
                .childName(childName)
                .totalSessions(totalSessions)
                .totalTrials(totalTrials)
                .totalSuccesses(totalSuccesses)
                .averageAccuracy(averageAccuracy)
                .sessionNotes(notes)
                .goalDetails(List.of())
                .build();

        return generateParentSummary(context);
    }

    /**
     * OpenAI API 호출
     */
    private String callOpenAiApi(String systemPrompt, String userPrompt) {
        log.debug("OpenAI API 호출 - 모델: {}", model);

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "max_tokens", maxTokens,
                "temperature", 0.7
        );

        try {
            Map<String, Object> response = webClient.post()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(TIMEOUT)
                    .block();

            if (response != null && response.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    String content = (String) message.get("content");
                    log.info("OpenAI API 응답 성공 ({}자)", content.length());
                    return content;
                }
            }

            throw new RuntimeException("OpenAI API 응답 파싱 실패");

        } catch (WebClientResponseException e) {
            log.error("OpenAI API 오류: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("OpenAI API 호출 실패: " + e.getMessage());
        }
    }

    /**
     * 시스템 프롬프트 생성
     */
    private String buildSystemPrompt() {
        return """
                당신은 발달장애 아동 ABA(응용행동분석) 치료 전문가입니다.

                역할:
                - 부모님께 전달할 치료 상담 일지를 작성합니다.
                - 따뜻하고 격려하는 어조를 사용합니다.
                - 전문 용어는 쉽게 풀어서 설명합니다.
                - 아이의 노력과 발전을 긍정적으로 강조합니다.

                작성 가이드라인:
                1. 첫 문장은 인사와 함께 전체적인 긍정적 평가로 시작
                2. 각 목표별 구체적인 발전 상황 설명
                3. 가정에서 할 수 있는 간단한 활동 제안
                4. 격려와 응원의 마무리 문장

                분량: 300~400자 내외
                """;
    }

    /**
     * 부모용 리포트 프롬프트 생성
     */
    private String buildParentSummaryPrompt(ParentReportContext ctx) {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("## 아동 정보\n- 이름: %s\n\n", ctx.getChildName()));

        sb.append(String.format("""
                ## 기간 통계
                - 총 세션 수: %d회
                - 총 시행 횟수: %d회
                - 총 성공 횟수: %d회
                - 평균 정반응률: %s

                """,
                ctx.getTotalSessions(),
                ctx.getTotalTrials(),
                ctx.getTotalSuccesses(),
                ctx.getAverageAccuracy() != null ? ctx.getAverageAccuracy() + "%" : "데이터 없음"
        ));

        // 목표별 상세 정보
        if (ctx.getGoalDetails() != null && !ctx.getGoalDetails().isEmpty()) {
            sb.append("## 목표별 달성 현황\n");
            for (GoalDetail goal : ctx.getGoalDetails()) {
                sb.append(String.format("""
                        ### %s (%s)
                        - 목표 성공률: %d%%
                        - 실제 달성률: %s%%
                        - 시행 횟수: %d회, 성공: %d회
                        - 주로 사용한 촉구: %s

                        """,
                        goal.getGoalName(),
                        goal.getCategory(),
                        goal.getTargetSuccessRate(),
                        goal.getActualSuccessRate() != null ? goal.getActualSuccessRate().toPlainString() : "N/A",
                        goal.getTotalTrials(),
                        goal.getTotalSuccesses(),
                        goal.getPrimaryPromptType()
                ));
            }
        }

        // 세션 노트
        if (StringUtils.hasText(ctx.getSessionNotes())) {
            sb.append(String.format("## 치료사 노트\n%s\n\n", ctx.getSessionNotes()));
        }

        sb.append("""
                ## 요청사항
                위 데이터를 바탕으로 부모님께 드릴 치료 상담 일지를 작성해 주세요.
                - 아이의 구체적인 발전 상황을 설명해 주세요.
                - 각 목표의 달성률을 쉽게 풀어서 설명해 주세요.
                - 가정에서 연습할 수 있는 간단한 활동을 1~2개 제안해 주세요.
                - 따뜻하고 격려하는 어조로 마무리해 주세요.
                """);

        return sb.toString();
    }

    /**
     * 더미 부모용 리포트 생성
     */
    private String generateDummyParentSummary(ParentReportContext ctx) {
        String accuracyStr = ctx.getAverageAccuracy() != null
                ? ctx.getAverageAccuracy().toPlainString()
                : "N/A";

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("""
                안녕하세요, %s 아동의 치료 상담 일지입니다.

                이번 기간 동안 %s 아동은 총 %d회의 세션에 참여하였으며, 평균 정반응률 %s%%를 보여주었습니다.
                """,
                ctx.getChildName(),
                ctx.getChildName(),
                ctx.getTotalSessions(),
                accuracyStr
        ));

        // 목표별 코멘트
        if (ctx.getGoalDetails() != null && !ctx.getGoalDetails().isEmpty()) {
            sb.append("\n[목표별 발전 상황]\n");
            for (GoalDetail goal : ctx.getGoalDetails()) {
                String status = goal.getActualSuccessRate() != null
                        && goal.getActualSuccessRate().compareTo(BigDecimal.valueOf(goal.getTargetSuccessRate())) >= 0
                        ? "목표 달성!" : "꾸준히 발전 중";
                sb.append(String.format("• %s: %s%% 달성 (%s)\n",
                        goal.getGoalName(),
                        goal.getActualSuccessRate() != null ? goal.getActualSuccessRate().toPlainString() : "N/A",
                        status
                ));
            }
        }

        sb.append("""

                가정에서는 일상적인 활동 중 간단한 지시 따르기 연습을 해주시면 좋겠습니다.
                예를 들어 "컵 가져와" 같은 한 단계 지시부터 시작해 보세요.

                아이가 보여주는 작은 발전에도 칭찬과 격려를 아끼지 말아 주세요.
                함께 노력하면 분명 더 큰 발전이 있을 거예요. 감사합니다!

                ※ 이 내용은 AI API 키가 설정되지 않아 생성된 예시 텍스트입니다.
                """);

        return sb.toString();
    }

    /**
     * 부모 리포트 생성을 위한 컨텍스트 DTO
     */
    @lombok.Builder
    @lombok.Getter
    public static class ParentReportContext {
        private String childName;
        private int totalSessions;
        private int totalTrials;
        private int totalSuccesses;
        private BigDecimal averageAccuracy;
        private String sessionNotes;
        private List<GoalDetail> goalDetails;
    }

    /**
     * 목표별 상세 정보 DTO
     */
    @lombok.Builder
    @lombok.Getter
    public static class GoalDetail {
        private String goalName;
        private String category;
        private int targetSuccessRate;
        private BigDecimal actualSuccessRate;
        private int totalTrials;
        private int totalSuccesses;
        private String primaryPromptType;
    }

    // ==================== 구조화된 JSON 리포트 생성 (고도화) ====================

    /**
     * 구조화된 JSON 형식의 AI 리포트 생성
     * - 프론트엔드에서 차트를 그릴 수 있는 데이터 포함
     * - summary, strength_weakness, recommendation, chart_data 구조
     */
    public AiReportContent generateStructuredReport(StructuredReportContext context) {
        if (!isApiKeyConfigured()) {
            log.info("OpenAI API 키가 설정되지 않았습니다. 더미 데이터를 사용합니다.");
            return generateDummyStructuredReport(context);
        }

        try {
            String systemPrompt = buildStructuredReportSystemPrompt();
            String userPrompt = buildStructuredReportUserPrompt(context);
            String jsonResponse = callOpenAiApiWithJsonFormat(systemPrompt, userPrompt);
            return parseStructuredReportResponse(jsonResponse, context);
        } catch (Exception e) {
            log.warn("OpenAI API 호출 실패. 더미 데이터를 사용합니다. 원인: {}", e.getMessage());
            return generateDummyStructuredReport(context);
        }
    }

    /**
     * JSON 포맷 응답을 강제하는 OpenAI API 호출
     */
    private String callOpenAiApiWithJsonFormat(String systemPrompt, String userPrompt) {
        log.debug("OpenAI API 호출 (JSON 포맷) - 모델: {}", model);

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "max_tokens", maxTokens,
                "temperature", 0.7,
                "response_format", Map.of("type", "json_object")  // JSON 응답 강제
        );

        try {
            Map<String, Object> response = webClient.post()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(TIMEOUT)
                    .block();

            if (response != null && response.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    String content = (String) message.get("content");
                    log.info("OpenAI API JSON 응답 성공 ({}자)", content.length());
                    log.debug("AI JSON 응답: {}", content);
                    return content;
                }
            }

            throw new RuntimeException("OpenAI API 응답 파싱 실패");

        } catch (WebClientResponseException e) {
            log.error("OpenAI API 오류: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("OpenAI API 호출 실패: " + e.getMessage());
        }
    }

    /**
     * 구조화된 리포트 시스템 프롬프트
     */
    private String buildStructuredReportSystemPrompt() {
        return """
                You are an expert ABA (Applied Behavior Analysis) therapist and report writer for children with developmental disabilities.

                Your task is to analyze therapy session data and generate a structured JSON report for parents.

                CRITICAL REQUIREMENTS:
                1. You MUST respond ONLY with valid JSON - no markdown, no explanations
                2. Use warm, encouraging, and professional Korean language
                3. Avoid jargon - explain technical terms simply
                4. Focus on positive progress while honestly addressing areas for improvement

                RESPONSE FORMAT (strict JSON):
                {
                  "summary": "전반적인 아동의 발달 상태 및 세션 수행 요약 (3~4문장, 한국어)",
                  "strength_weakness": "주요 강점과 보완이 필요한 부분 분석 (한국어)",
                  "recommendation": "가정 연계 활동 및 다음 치료 방향 제안 (구체적인 활동 1~2개 포함, 한국어)",
                  "chart_data": [
                    {"date": "YYYY-MM-DD", "score": 수행정확도숫자}
                  ]
                }

                IMPORTANT:
                - chart_data must contain the EXACT session data provided by the user (do not modify dates or scores)
                - summary should be 3-4 sentences highlighting overall progress
                - strength_weakness should identify 1-2 strengths and 1-2 areas for improvement
                - recommendation should suggest specific home activities parents can do
                """;
    }

    /**
     * 구조화된 리포트 사용자 프롬프트 생성
     */
    private String buildStructuredReportUserPrompt(StructuredReportContext ctx) {
        StringBuilder sb = new StringBuilder();

        sb.append("## 아동 정보\n");
        sb.append(String.format("- 이름: %s\n", ctx.getChildName()));
        sb.append(String.format("- 리포트 기간: %s ~ %s\n\n", ctx.getPeriodStart(), ctx.getPeriodEnd()));

        sb.append("## 기간 내 전체 통계\n");
        sb.append(String.format("- 총 세션 수: %d회\n", ctx.getTotalSessions()));
        sb.append(String.format("- 총 시행 횟수: %d회\n", ctx.getTotalTrials()));
        sb.append(String.format("- 총 성공 횟수: %d회\n", ctx.getTotalSuccesses()));
        sb.append(String.format("- 전체 평균 정반응률: %s%%\n\n",
                ctx.getAverageAccuracy() != null ? ctx.getAverageAccuracy().toPlainString() : "N/A"));

        // 날짜별 세션 데이터 (차트용)
        sb.append("## 날짜별 세션 수행 데이터 (최근 10개)\n");
        sb.append("이 데이터를 chart_data 필드에 그대로 포함해주세요:\n");
        if (ctx.getSessionScores() != null && !ctx.getSessionScores().isEmpty()) {
            for (SessionScore score : ctx.getSessionScores()) {
                sb.append(String.format("- %s: %s%%\n", score.getDate(), score.getScore().toPlainString()));
            }
        } else {
            sb.append("- 세션 데이터 없음\n");
        }
        sb.append("\n");

        // 목표별 상세 정보
        if (ctx.getGoalDetails() != null && !ctx.getGoalDetails().isEmpty()) {
            sb.append("## 목표별 달성 현황\n");
            for (GoalDetail goal : ctx.getGoalDetails()) {
                sb.append(String.format("### %s (%s)\n", goal.getGoalName(), goal.getCategory()));
                sb.append(String.format("- 목표 성공률: %d%%\n", goal.getTargetSuccessRate()));
                sb.append(String.format("- 실제 달성률: %s%%\n",
                        goal.getActualSuccessRate() != null ? goal.getActualSuccessRate().toPlainString() : "N/A"));
                sb.append(String.format("- 시행 횟수: %d회, 성공: %d회\n\n",
                        goal.getTotalTrials(), goal.getTotalSuccesses()));
            }
        }

        // 세션 노트
        if (StringUtils.hasText(ctx.getSessionNotes())) {
            sb.append("## 치료사 노트\n");
            sb.append(ctx.getSessionNotes());
            sb.append("\n\n");
        }

        sb.append("""
                ## 요청사항
                위 데이터를 바탕으로 부모님께 드릴 구조화된 리포트를 JSON 형식으로 작성해주세요.
                - summary: 따뜻한 인사와 함께 전체적인 발전 상황 요약 (3~4문장)
                - strength_weakness: 아이의 강점과 개선이 필요한 부분 분석
                - recommendation: 가정에서 할 수 있는 구체적인 연습 활동 1~2개 제안
                - chart_data: 위에서 제공한 날짜별 수행 데이터를 그대로 포함
                """);

        return sb.toString();
    }

    /**
     * AI JSON 응답 파싱
     */
    private AiReportContent parseStructuredReportResponse(String jsonResponse, StructuredReportContext context) {
        try {
            AiReportContent parsed = objectMapper.readValue(jsonResponse, AiReportContent.class);

            // chart_data가 비어있거나 null이면 원본 데이터로 채움
            if (parsed.getChartData() == null || parsed.getChartData().isEmpty()) {
                log.warn("AI 응답에 chart_data가 없습니다. 원본 세션 데이터로 대체합니다.");
                List<AiReportContent.ChartDataPoint> chartData = context.getSessionScores().stream()
                        .map(s -> AiReportContent.ChartDataPoint.builder()
                                .date(s.getDate().toString())
                                .score(s.getScore())
                                .build())
                        .toList();

                return AiReportContent.builder()
                        .summary(parsed.getSummary())
                        .strengthWeakness(parsed.getStrengthWeakness())
                        .recommendation(parsed.getRecommendation())
                        .chartData(chartData)
                        .build();
            }

            return parsed;
        } catch (JsonProcessingException e) {
            log.error("AI JSON 응답 파싱 실패: {}", jsonResponse);
            throw new RuntimeException("AI 응답 JSON 파싱 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 더미 구조화된 리포트 생성 (API 키 미설정 시)
     */
    private AiReportContent generateDummyStructuredReport(StructuredReportContext ctx) {
        String accuracyStr = ctx.getAverageAccuracy() != null
                ? ctx.getAverageAccuracy().toPlainString()
                : "N/A";

        // 차트 데이터 생성
        List<AiReportContent.ChartDataPoint> chartData = List.of();
        if (ctx.getSessionScores() != null && !ctx.getSessionScores().isEmpty()) {
            chartData = ctx.getSessionScores().stream()
                    .map(s -> AiReportContent.ChartDataPoint.builder()
                            .date(s.getDate().toString())
                            .score(s.getScore())
                            .build())
                    .toList();
        }

        return AiReportContent.builder()
                .summary(String.format(
                        "안녕하세요, %s 아동의 치료 상담 일지입니다. " +
                        "이번 기간 동안 %s 아동은 총 %d회의 세션에 참여하였으며, 평균 정반응률 %s%%를 보여주었습니다. " +
                        "전반적으로 꾸준한 발전을 보이고 있으며, 특히 촉구에 대한 반응이 좋아지고 있습니다.",
                        ctx.getChildName(), ctx.getChildName(), ctx.getTotalSessions(), accuracyStr))
                .strengthWeakness(
                        "강점: 언어적 촉구에 대한 반응이 빠르고, 치료사와의 라포가 잘 형성되어 있습니다. " +
                        "보완점: 새로운 환경이나 과제에 대한 적응 시간이 필요하며, 독립적 수행 비율을 높이는 것이 목표입니다.")
                .recommendation(
                        "가정에서는 다음 활동을 추천드립니다: " +
                        "1) 식사 시간에 '눈 맞추고 인사하기' 연습 - 밥 먹기 전 눈을 맞추고 '잘 먹겠습니다'라고 말하면 칭찬해주세요. " +
                        "2) 간단한 심부름 놀이 - '휴지 가져와' 같은 한 단계 지시를 주고, 성공하면 하이파이브로 칭찬해주세요. " +
                        "※ 이 내용은 AI API 키가 설정되지 않아 생성된 예시 텍스트입니다.")
                .chartData(chartData)
                .build();
    }

    /**
     * 구조화된 리포트 생성을 위한 컨텍스트 DTO
     */
    @lombok.Builder
    @lombok.Getter
    public static class StructuredReportContext {
        private String childName;
        private LocalDate periodStart;
        private LocalDate periodEnd;
        private int totalSessions;
        private int totalTrials;
        private int totalSuccesses;
        private BigDecimal averageAccuracy;
        private String sessionNotes;
        private List<GoalDetail> goalDetails;
        private List<SessionScore> sessionScores;  // 날짜별 세션 점수 (차트용)
    }

    /**
     * 날짜별 세션 점수 DTO
     */
    @lombok.Builder
    @lombok.Getter
    public static class SessionScore {
        private LocalDate date;
        private BigDecimal score;  // 해당 날짜의 평균 수행 정확도 (%)
    }

    // ==================== Vision API 관련 메서드 ====================

    /**
     * Vision API를 사용하여 이미지에서 아동 데이터 추출
     * @param base64Image Base64로 인코딩된 이미지 문자열
     * @param mimeType 이미지 MIME 타입 (image/jpeg, image/png 등)
     * @return 추출된 아동 데이터
     */
    public ExtractedChildData extractChildDataFromImage(String base64Image, String mimeType) {
        if (!isApiKeyConfigured()) {
            log.warn("OpenAI API 키가 설정되지 않았습니다. 더미 데이터를 반환합니다.");
            return generateDummyExtractedData();
        }

        try {
            String jsonResponse = callVisionApi(base64Image, mimeType);
            return parseExtractedData(jsonResponse);
        } catch (Exception e) {
            log.error("Vision API 호출 또는 파싱 실패: {}", e.getMessage());
            throw new RuntimeException("이미지에서 데이터를 추출할 수 없습니다: " + e.getMessage(), e);
        }
    }

    /**
     * Vision API 호출
     */
    private String callVisionApi(String base64Image, String mimeType) {
        log.info("Vision API 호출 시작 - 모델: {}", model);

        String systemPrompt = buildVisionSystemPrompt();
        String dataUrl = String.format("data:%s;base64,%s", mimeType, base64Image);

        // Vision API용 메시지 구성
        Map<String, Object> imageContent = Map.of(
                "type", "image_url",
                "image_url", Map.of(
                        "url", dataUrl,
                        "detail", "high"  // 고해상도 분석
                )
        );

        Map<String, Object> textContent = Map.of(
                "type", "text",
                "text", buildVisionUserPrompt()
        );

        Map<String, Object> userMessage = Map.of(
                "role", "user",
                "content", List.of(textContent, imageContent)
        );

        Map<String, Object> systemMessage = Map.of(
                "role", "system",
                "content", systemPrompt
        );

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(systemMessage, userMessage),
                "max_tokens", 2000,
                "temperature", 0.1,  // 정확한 추출을 위해 낮은 temperature
                "response_format", Map.of("type", "json_object")  // JSON 응답 보장
        );

        try {
            Map<String, Object> response = webClient.post()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(VISION_TIMEOUT)
                    .block();

            if (response != null && response.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    String content = (String) message.get("content");
                    log.info("Vision API 응답 성공 ({}자)", content.length());
                    // Base64 이미지는 로그에 남기지 않음 (보안)
                    log.debug("추출된 JSON: {}", content);
                    return content;
                }
            }

            throw new RuntimeException("Vision API 응답 파싱 실패");

        } catch (WebClientResponseException e) {
            log.error("Vision API 오류: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Vision API 호출 실패: " + e.getMessage());
        }
    }

    /**
     * Vision API 시스템 프롬프트
     */
    private String buildVisionSystemPrompt() {
        return """
                You are an expert Data Entry Clerk for DearOne Therapy, a therapy center for children with developmental disabilities.
                Your task is to analyze handwritten ABA (Applied Behavior Analysis) therapy session records and extract structured data.

                IMPORTANT RULES:
                1. Extract information EXACTLY as written in the document
                2. If a field is unclear or not present, use null
                3. For dates, convert to YYYY-MM-DD format
                4. For gender, use "MALE" or "FEMALE"
                5. Return ONLY valid JSON - no explanations or markdown

                CATEGORY MAPPING (Korean to English):
                - 사회성, 사회 → SOCIAL
                - 의사소통, 언어 → COMMUNICATION
                - 행동, 문제행동 → BEHAVIOR
                - 인지, 학습 → COGNITIVE
                - 감각, 감각통합 → SENSORY
                - 자조, 신변처리 → SELF_CARE
                - 운동, 대근육, 소근육 → MOTOR
                - 놀이 → PLAY

                STATUS MAPPING:
                - 진행중, 수행중 → IN_PROGRESS
                - 대기, 미시작 → WAITING
                - 완료, 달성 → COMPLETED
                - 유지 → MAINTENANCE
                """;
    }

    /**
     * Vision API 사용자 프롬프트
     */
    private String buildVisionUserPrompt() {
        return """
                이 이미지는 발달센터의 아동 치료세션 수기 기록지입니다.

                다음 정보를 추출하여 JSON 형식으로 반환해 주세요:

                1. childName: 아동의 이름
                2. birthDate: 생년월일 (YYYY-MM-DD 형식)
                3. gender: 성별 (MALE 또는 FEMALE)
                4. diagnosis: 진단명 (예: ASD, ADHD, 발달지연 등)
                5. goals: 치료 목표 배열
                   - title: 목표 내용
                   - category: 카테고리 (SOCIAL, COMMUNICATION, BEHAVIOR, COGNITIVE, SENSORY, SELF_CARE, MOTOR, PLAY 중 하나)
                   - status: 상태 (IN_PROGRESS, WAITING, COMPLETED, MAINTENANCE 중 하나)
                   - description: 추가 설명 (있는 경우)

                JSON 형식 예시:
                {
                  "childName": "홍길동",
                  "birthDate": "2020-01-15",
                  "gender": "MALE",
                  "diagnosis": "ASD",
                  "goals": [
                    {
                      "title": "눈 맞춤 3초 유지하기",
                      "category": "SOCIAL",
                      "status": "IN_PROGRESS",
                      "description": "치료사 호명 시 눈 맞춤"
                    }
                  ]
                }

                주의: 이미지에서 읽을 수 없거나 불명확한 정보는 null로 설정하세요.
                반드시 유효한 JSON만 반환하세요.
                """;
    }

    /**
     * JSON 응답을 ExtractedChildData로 파싱
     */
    private ExtractedChildData parseExtractedData(String jsonResponse) {
        try {
            return objectMapper.readValue(jsonResponse, ExtractedChildData.class);
        } catch (JsonProcessingException e) {
            log.error("JSON 파싱 실패. 응답: {}", jsonResponse);
            throw new RuntimeException("AI 응답 JSON 파싱 실패: " + e.getMessage(), e);
        }
    }

    /**
     * API 키 없을 때 테스트용 더미 데이터 생성
     */
    private ExtractedChildData generateDummyExtractedData() {
        return ExtractedChildData.builder()
                .childName("테스트 아동")
                .birthDate("2020-01-01")
                .gender("MALE")
                .diagnosis("ASD (테스트 데이터)")
                .goals(List.of(
                        ExtractedChildData.ExtractedGoal.builder()
                                .title("눈 맞춤 유지하기")
                                .category("SOCIAL")
                                .status("IN_PROGRESS")
                                .description("API 키 미설정으로 생성된 더미 목표")
                                .build(),
                        ExtractedChildData.ExtractedGoal.builder()
                                .title("지시 따르기")
                                .category("BEHAVIOR")
                                .status("WAITING")
                                .description("API 키 미설정으로 생성된 더미 목표")
                                .build()
                ))
                .build();
    }
}
