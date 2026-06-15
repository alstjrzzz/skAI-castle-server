package com.ccp.skAI_castle_server.service;

import com.ccp.skAI_castle_server.domain.QuestionType;
import com.ccp.skAI_castle_server.domain.entity.ChatMessage;
import com.ccp.skAI_castle_server.dto.response.OutlineDto;
import com.ccp.skAI_castle_server.exception.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.ccp.skAI_castle_server.dto.response.ApiResultCode.EXTERNAL_API_ERROR;

@Slf4j
@Service
public class LlmService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public record QuestionData(
            String question,
            String modelAnswer,
            List<String> keywords,
            QuestionType questionType,
            String choicesJson
    ) {}

    public LlmService(
            @Qualifier("aiServerRestClient") RestClient restClient,
            ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    private String callChat(List<Map<String, String>> messages) {
        Map<String, Object> body = Map.of("messages", messages);
        try {
            String response = restClient.post()
                    .uri("/v1/llm/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return objectMapper.readTree(response).path("content").asText();
        } catch (Exception e) {
            throw new ApiException(EXTERNAL_API_ERROR, e);
        }
    }

    private String stripCodeBlock(String text) {
        String s = text.strip();
        if (s.startsWith("```")) {
            int newline = s.indexOf('\n');
            if (newline >= 0) s = s.substring(newline + 1);
            if (s.endsWith("```")) s = s.substring(0, s.lastIndexOf("```")).stripTrailing();
        }
        return s;
    }

    public OutlineDto generateOutline(String topicTitle) {
        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content",
                        "당신은 교육 커리큘럼 설계자입니다. 학습 목차를 JSON 형식으로 생성하세요. " +
                        "챕터 제목과 키워드는 반드시 한국어로 작성하세요. " +
                        "아래 JSON 형식만 반환하세요: " +
                        "{\"chapters\": [{\"title\": \"챕터 제목\", \"keywords\": [\"키워드1\", \"키워드2\"]}]}"),
                Map.of("role", "user", "content",
                        "다음 주제의 학습 목차를 한국어로 생성하세요: " + topicTitle +
                        "\n챕터 4~6개, 각 챕터당 키워드 3~5개를 포함하세요.")
        );
        try {
            String json = stripCodeBlock(callChat(messages));
            return objectMapper.readValue(json, OutlineDto.class);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(EXTERNAL_API_ERROR, e);
        }
    }

    public String tutorChat(String topicTitle, String outlineJson, List<ChatMessage> history) {
        List<Map<String, String>> messages = new ArrayList<>();

        String systemPrompt = "당신은 학습자가 '" + topicTitle + "'을(를) 이해할 수 있도록 돕는 AI 튜터입니다.\n" +
                "소크라테스식 대화법으로 학습자를 이끄세요. 핵심 개념을 명확하게 설명하고, 예시를 들어주고, " +
                "깊이 생각할 수 있도록 질문하세요. 모든 답변은 반드시 한국어로 작성하세요.";
        if (outlineJson != null) {
            systemPrompt += "\n\n학습 목차 (참고용):\n" + outlineJson;
        }
        messages.add(Map.of("role", "system", "content", systemPrompt));

        for (ChatMessage msg : history) {
            messages.add(Map.of("role", msg.getRole().name().toLowerCase(), "content", msg.getContent()));
        }

        return callChat(messages);
    }

    public List<QuestionData> generateQuestions(String outlineJson, List<ChatMessage> history) {
        StringBuilder historyStr = new StringBuilder();
        for (ChatMessage msg : history) {
            if (msg.getRole().name().equals("SYSTEM")) continue;
            historyStr.append(msg.getRole().name()).append(": ").append(msg.getContent()).append("\n");
        }

        String systemPrompt =
                "당신은 교육 평가 전문가입니다. 튜터링 세션을 기반으로 역설명(recall) 질문을 생성하세요. " +
                "서술형(WRITTEN)과 객관식(MULTIPLE_CHOICE) 두 가지 유형의 질문을 생성하세요. " +
                "모든 질문, 답변, 키워드는 반드시 한국어로 작성하세요. " +
                "아래 JSON 형식만 반환하세요: " +
                "{\"questions\": [" +
                "{\"type\": \"WRITTEN\", \"question\": \"질문\", \"modelAnswer\": \"모범 답안\", \"keywords\": [\"키워드1\"]}," +
                "{\"type\": \"MULTIPLE_CHOICE\", \"question\": \"질문\", \"modelAnswer\": \"정답 설명\", \"keywords\": [], " +
                "\"choices\": [{\"label\": \"A\", \"text\": \"선지\", \"isCorrect\": true}, {\"label\": \"B\", \"text\": \"선지\", \"isCorrect\": false}, " +
                "{\"label\": \"C\", \"text\": \"선지\", \"isCorrect\": false}, {\"label\": \"D\", \"text\": \"선지\", \"isCorrect\": false}]}" +
                "]}";

        String userPrompt =
                "학습 목차: " + outlineJson + "\n\n" +
                "대화 기록:\n" + historyStr + "\n\n" +
                "위 대화에서 다룬 핵심 개념을 확인할 수 있는 질문 10개를 한국어로 생성하세요. " +
                "서술형(WRITTEN) 5~6개, 객관식(MULTIPLE_CHOICE) 4~5개로 구성하세요. " +
                "서술형: '왜?', '어떻게?', '차이점은?', '자신의 말로 설명하시오' 같은 개방형 질문. 모범 답안과 키워드 3~5개 포함. " +
                "객관식: 개념 이해를 확인하는 4지선다 문제. 정답 1개, 그럴듯한 오답 3개. " +
                "대화 중 튜터가 이미 직접 물어본 질문을 그대로 반복하지 마세요.";

        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        );

        try {
            String json = stripCodeBlock(callChat(messages));
            JsonNode root = objectMapper.readTree(json);
            JsonNode questionsNode = root.isArray() ? root : root.path("questions");

            List<QuestionData> result = new ArrayList<>();
            for (JsonNode item : questionsNode) {
                String question = item.path("question").asText();
                String modelAnswer = item.path("modelAnswer").asText();
                List<String> keywords = new ArrayList<>();
                for (JsonNode kw : item.path("keywords")) keywords.add(kw.asText());

                QuestionType questionType = QuestionType.WRITTEN;
                String typeStr = item.path("type").asText("");
                if ("MULTIPLE_CHOICE".equals(typeStr)) {
                    questionType = QuestionType.MULTIPLE_CHOICE;
                }

                String choicesJson = null;
                if (questionType == QuestionType.MULTIPLE_CHOICE && item.has("choices")) {
                    choicesJson = objectMapper.writeValueAsString(item.path("choices"));
                }

                result.add(new QuestionData(question, modelAnswer, keywords, questionType, choicesJson));
            }
            return result;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(EXTERNAL_API_ERROR, e);
        }
    }

    public String generateFeedback(int score, List<String> questions, List<String> modelAnswers, List<String> userAnswers) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < questions.size(); i++) {
            sb.append("Q").append(i + 1).append(": ").append(questions.get(i)).append("\n");
            sb.append("모범 답안: ").append(modelAnswers.get(i)).append("\n");
            sb.append("학생 답변: ").append(userAnswers.get(i)).append("\n\n");
        }

        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content",
                        "당신은 학습자를 응원하는 학습 코치입니다. 역설명 평가 결과에 대해 한국어로 짧고 건설적인 피드백을 제공하세요."),
                Map.of("role", "user", "content",
                        "총점: " + score + "/100\n\n" + sb +
                        "잘한 점을 인정하고 개선할 부분을 제안하는 2~3문장의 피드백을 한국어로 작성하세요. 격려하되 구체적으로 작성하세요.")
        );

        try {
            return callChat(messages);
        } catch (Exception e) {
            log.warn("Feedback generation failed, returning default feedback", e);
            return "총점 " + score + "점입니다. 복습을 통해 부족한 부분을 보완해 보세요.";
        }
    }
}
