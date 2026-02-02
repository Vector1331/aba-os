package com.aba.os.abaosserver.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class OpenAiService {

    private final WebClient webClient;
    private final String apiKey;
    private final String model;
    private final int maxTokens;

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";

    public OpenAiService(
            @Value("${openai.api-key:}") String apiKey,
            @Value("${openai.model:gpt-3.5-turbo}") String model,
            @Value("${openai.max-tokens:500}") int maxTokens
    ) {
        this.apiKey = apiKey;
        this.model = model;
        this.maxTokens = maxTokens;
        this.webClient = WebClient.builder()
                .baseUrl(OPENAI_API_URL)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * AI 기반 치료 리포트 코멘트 생성
     * - API 키가 없거나 호출 실패 시 더미 데이터 반환
     */
    public String generateReportComment(
            String childName,
            int totalSessions,
            int totalTrials,
            int totalSuccesses,
            BigDecimal averageAccuracy,
            String notes
    ) {
        // API 키가 없으면 더미 데이터 반환
        if (!StringUtils.hasText(apiKey)) {
            log.info("OpenAI API 키가 설정되지 않았습니다. 더미 데이터를 사용합니다.");
            return generateDummyComment(childName, averageAccuracy);
        }

        try {
            String prompt = buildPrompt(childName, totalSessions, totalTrials, totalSuccesses, averageAccuracy, notes);
            return callOpenAiApi(prompt);
        } catch (Exception e) {
            log.warn("OpenAI API 호출 실패. 더미 데이터를 사용합니다. 원인: {}", e.getMessage());
            return generateDummyComment(childName, averageAccuracy);
        }
    }

    /**
     * OpenAI API 호출
     */
    private String callOpenAiApi(String prompt) {
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", "당신은 발달장애 아동 치료 전문가입니다. 부모님께 전달할 따뜻하고 전문적인 치료 소견을 작성해 주세요."),
                        Map.of("role", "user", "content", prompt)
                ),
                "max_tokens", maxTokens,
                "temperature", 0.7
        );

        Map<String, Object> response = webClient.post()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response != null && response.containsKey("choices")) {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (!choices.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                return (String) message.get("content");
            }
        }

        throw new RuntimeException("OpenAI API 응답 파싱 실패");
    }

    /**
     * 프롬프트 생성
     */
    private String buildPrompt(
            String childName,
            int totalSessions,
            int totalTrials,
            int totalSuccesses,
            BigDecimal averageAccuracy,
            String notes
    ) {
        String accuracyStr = averageAccuracy != null ? averageAccuracy.toPlainString() + "%" : "데이터 없음";

        return String.format("""
                아동 이름: %s
                통계 정보:
                - 총 세션 수: %d회
                - 총 시행 횟수: %d회
                - 총 성공 횟수: %d회
                - 평균 수행 정확도: %s

                세션 노트: %s

                위 정보를 바탕으로 부모님께 드릴 치료 총평을 300자 내외로 부드러운 어조로 작성해 주세요.
                아이의 발전 상황을 긍정적으로 설명하고, 향후 치료 방향에 대한 제안을 포함해 주세요.
                """,
                childName,
                totalSessions,
                totalTrials,
                totalSuccesses,
                accuracyStr,
                notes != null ? notes : "없음"
        );
    }

    /**
     * 더미 코멘트 생성 (API 키 없음 또는 호출 실패 시)
     */
    private String generateDummyComment(String childName, BigDecimal averageAccuracy) {
        String accuracyStr = averageAccuracy != null ? averageAccuracy.toPlainString() : "N/A";

        return String.format("""
                [AI 요약 예시]
                (테스트 모드) %s 아동의 수행률(%s%%)이 매우 긍정적입니다.

                최근 세션에서 보여준 집중력과 과제 수행 능력이 눈에 띄게 향상되었습니다.
                특히 언어적 촉구에 대한 반응이 좋아지고 있으며, 자발적인 눈 맞춤 시도가 증가하고 있습니다.

                향후 치료 방향으로는 시각적 보조를 점진적으로 줄이고, 자연스러운 상황에서의 일반화 훈련을 권장합니다.
                가정에서도 일상적인 활동 중 간단한 지시 따르기 연습을 해주시면 더욱 효과적입니다.

                ※ 이 내용은 AI API 키가 설정되지 않아 생성된 예시 텍스트입니다.
                """,
                childName,
                accuracyStr
        );
    }
}
