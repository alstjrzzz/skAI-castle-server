package com.ccp.skAI_castle_server.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ScoringService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public ScoringService(
            @Qualifier("aiServerRestClient") RestClient restClient,
            ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    public int score(String question, String userAnswer, String modelAnswer, String keywordsJson) {
        List<String> keywords = parseKeywords(keywordsJson);

        Map<String, Object> body = Map.of(
                "question", question,
                "modelAnswer", modelAnswer,
                "studentAnswer", userAnswer,
                "keywords", keywords
        );

        try {
            String response = restClient.post()
                    .uri("/v1/grading/score")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            double totalScore = objectMapper.readTree(response)
                    .path("scores").path("totalScore").asDouble(0.0);
            return (int) Math.min(100, Math.max(0, Math.round(totalScore)));
        } catch (Exception e) {
            log.warn("Grading API call failed, returning 0", e);
            return 0;
        }
    }

    private List<String> parseKeywords(String keywordsJson) {
        if (keywordsJson == null || keywordsJson.isBlank()) return List.of();
        try {
            return objectMapper.readValue(keywordsJson, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Failed to parse keywords JSON: {}", keywordsJson);
            return List.of();
        }
    }
}
