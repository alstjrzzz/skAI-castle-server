package com.ccp.skAI_castle_server.service;

import com.ccp.skAI_castle_server.domain.entity.ChatMessage;
import com.ccp.skAI_castle_server.dto.response.OutlineDto;
import com.ccp.skAI_castle_server.exception.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;
import java.util.stream.Collectors;

import static com.ccp.skAI_castle_server.dto.response.ApiResultCode.EXTERNAL_API_ERROR;

@Slf4j
@Service
public class LlmService {

    private final RestClient restClient;
    private final String chatModel;
    private final String embeddingModel;
    private final ObjectMapper objectMapper;

    public record QuestionData(String question, String modelAnswer, List<String> keywords) {}

    public LlmService(
            @Qualifier("openAiRestClient") RestClient restClient,
            @Value("${openai.chat-model:gpt-4o-mini}") String chatModel,
            @Value("${openai.embedding-model:text-embedding-3-small}") String embeddingModel,
            ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.chatModel = chatModel;
        this.embeddingModel = embeddingModel;
        this.objectMapper = objectMapper;
    }

    private String callChat(List<Map<String, String>> messages, boolean jsonMode) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", chatModel);
        body.put("messages", messages);
        if (jsonMode) {
            body.put("response_format", Map.of("type", "json_object"));
        }
        try {
            String response = restClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return objectMapper.readTree(response)
                    .path("choices").get(0).path("message").path("content").asText();
        } catch (Exception e) {
            throw new ApiException(EXTERNAL_API_ERROR, e);
        }
    }

    public OutlineDto generateOutline(String topicTitle) {
        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content",
                        "You are a curriculum designer. Generate a structured study outline in JSON format. " +
                        "Return ONLY valid JSON matching this exact structure: " +
                        "{\"chapters\": [{\"title\": \"Chapter Title\", \"keywords\": [\"keyword1\", \"keyword2\"]}]}"),
                Map.of("role", "user", "content",
                        "Create a comprehensive outline for studying: " + topicTitle +
                        "\nGenerate 4-6 chapters, each with 3-5 keywords.")
        );
        try {
            String json = callChat(messages, true);
            return objectMapper.readValue(json, OutlineDto.class);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(EXTERNAL_API_ERROR, e);
        }
    }

    public String tutorChat(String topicTitle, String outlineJson, List<ChatMessage> history) {
        List<Map<String, String>> messages = new ArrayList<>();

        String systemPrompt = "You are an expert AI tutor helping the student learn: " + topicTitle + ".\n" +
                "Guide them through Socratic dialogue — ask clarifying questions, explain concepts clearly, " +
                "provide examples, and encourage deeper thinking. Be patient and engaging.";
        if (outlineJson != null) {
            systemPrompt += "\n\nStudy outline (for your reference):\n" + outlineJson;
        }
        messages.add(Map.of("role", "system", "content", systemPrompt));

        for (ChatMessage msg : history) {
            messages.add(Map.of("role", msg.getRole().name().toLowerCase(), "content", msg.getContent()));
        }

        return callChat(messages, false);
    }

    public List<QuestionData> generateQuestions(String outlineJson, List<ChatMessage> history) {
        StringBuilder historyStr = new StringBuilder();
        for (ChatMessage msg : history) {
            if (msg.getRole().name().equals("SYSTEM")) continue;
            historyStr.append(msg.getRole().name()).append(": ").append(msg.getContent()).append("\n");
        }

        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content",
                        "You are an educational assessment expert. Generate recall questions based on a tutoring session. " +
                        "Return ONLY valid JSON object with key \"questions\" containing an array: " +
                        "{\"questions\": [{\"question\": \"...\", \"modelAnswer\": \"Detailed answer...\", " +
                        "\"keywords\": [\"key1\", \"key2\"]}]}"),
                Map.of("role", "user", "content",
                        "Study outline: " + outlineJson + "\n\n" +
                        "Chat history:\n" + historyStr + "\n\n" +
                        "Generate 3-5 comprehensive recall questions covering the key concepts discussed. " +
                        "Each question needs a detailed model answer and 3-5 key concept keywords.")
        );

        try {
            String json = callChat(messages, true);
            JsonNode root = objectMapper.readTree(json);
            JsonNode questionsNode = root.isArray() ? root : root.path("questions");

            List<QuestionData> result = new ArrayList<>();
            for (JsonNode item : questionsNode) {
                String question = item.path("question").asText();
                String modelAnswer = item.path("modelAnswer").asText();
                List<String> keywords = new ArrayList<>();
                for (JsonNode kw : item.path("keywords")) keywords.add(kw.asText());
                result.add(new QuestionData(question, modelAnswer, keywords));
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
            sb.append("Expected: ").append(modelAnswers.get(i)).append("\n");
            sb.append("Student: ").append(userAnswers.get(i)).append("\n\n");
        }

        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content",
                        "You are a supportive learning coach. Provide brief, constructive feedback on a student's recall evaluation."),
                Map.of("role", "user", "content",
                        "Score: " + score + "/100\n\n" + sb +
                        "Provide 2-3 sentences of feedback acknowledging strengths and suggesting improvements. Be encouraging and specific.")
        );

        try {
            return callChat(messages, false);
        } catch (Exception e) {
            log.warn("Feedback generation failed, returning default feedback", e);
            return "총점 " + score + "점입니다. 복습을 통해 부족한 부분을 보완해 보세요.";
        }
    }

    public List<double[]> getEmbeddings(List<String> texts) {
        if (texts.isEmpty()) return List.of();

        Map<String, Object> body = Map.of("model", embeddingModel, "input", texts);

        try {
            String response = restClient.post()
                    .uri("/embeddings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode data = objectMapper.readTree(response).path("data");
            double[][] result = new double[texts.size()][];
            for (JsonNode item : data) {
                int idx = item.path("index").asInt();
                JsonNode embNode = item.path("embedding");
                double[] emb = new double[embNode.size()];
                for (int j = 0; j < embNode.size(); j++) emb[j] = embNode.get(j).asDouble();
                result[idx] = emb;
            }
            return Arrays.asList(result);
        } catch (Exception e) {
            log.warn("Embedding API failed, falling back to zero vectors", e);
            return texts.stream().map(t -> new double[0]).collect(Collectors.toList());
        }
    }
}
