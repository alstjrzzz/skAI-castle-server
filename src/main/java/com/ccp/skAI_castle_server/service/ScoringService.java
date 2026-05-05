package com.ccp.skAI_castle_server.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScoringService {

    private final LlmService llmService;
    private final ObjectMapper objectMapper;

    public int score(String userAnswer, String modelAnswer, String keywordsJson) {
        List<String> keywords = parseKeywords(keywordsJson);

        // Batch embedding for efficiency: [userAnswer, modelAnswer]
        List<double[]> embeddings = llmService.getEmbeddings(List.of(userAnswer, modelAnswer));
        double[] userEmb = embeddings.get(0);
        double[] modelEmb = embeddings.get(1);

        double dense = (userEmb.length > 0 && modelEmb.length > 0)
                ? cosineSimilarity(userEmb, modelEmb) : 0.0;
        double sparse = bagOfWordsSimilarity(userAnswer, modelAnswer);
        double keyword = keywordMatchRatio(userAnswer, keywords);

        // Weighted sum: Dense 45%, Sparse 30%, Keyword 25%
        double effectiveDenseWeight = (dense > 0) ? 0.45 : 0.0;
        double remainder = 1.0 - effectiveDenseWeight;
        double weighted = dense * effectiveDenseWeight
                + sparse * (0.30 / (1.0 - 0.45) * remainder)
                + keyword * (0.25 / (1.0 - 0.45) * remainder);

        if (effectiveDenseWeight > 0) {
            weighted = dense * 0.45 + sparse * 0.30 + keyword * 0.25;
        }

        // Length correction
        weighted *= computeLengthFactor(userAnswer, modelAnswer);

        // Relevance cap: if overall semantic relevance is very low, cap the score
        double relevance = dense > 0 ? (dense * 0.6 + sparse * 0.4) : sparse;
        if (relevance < 0.15) {
            weighted = Math.min(weighted, 0.25);
        }

        return (int) Math.min(100, Math.max(0, Math.round(weighted * 100)));
    }

    private double cosineSimilarity(double[] a, double[] b) {
        if (a.length != b.length || a.length == 0) return 0.0;
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        double denom = Math.sqrt(normA) * Math.sqrt(normB);
        return denom == 0 ? 0.0 : Math.max(0, dot / denom);
    }

    private double bagOfWordsSimilarity(String text1, String text2) {
        Set<String> tokens1 = tokenize(text1);
        Set<String> tokens2 = tokenize(text2);
        if (tokens1.isEmpty() || tokens2.isEmpty()) return 0.0;

        Set<String> all = new HashSet<>(tokens1);
        all.addAll(tokens2);

        double dot = 0, norm1 = 0, norm2 = 0;
        for (String token : all) {
            double v1 = tokens1.contains(token) ? 1.0 : 0.0;
            double v2 = tokens2.contains(token) ? 1.0 : 0.0;
            dot += v1 * v2;
            norm1 += v1 * v1;
            norm2 += v2 * v2;
        }
        double denom = Math.sqrt(norm1) * Math.sqrt(norm2);
        return denom == 0 ? 0.0 : dot / denom;
    }

    private double keywordMatchRatio(String userAnswer, List<String> keywords) {
        if (keywords.isEmpty()) return 0.5;
        String lowerAnswer = userAnswer.toLowerCase();
        long matched = keywords.stream()
                .filter(kw -> lowerAnswer.contains(kw.toLowerCase()))
                .count();
        return (double) matched / keywords.size();
    }

    private double computeLengthFactor(String userAnswer, String modelAnswer) {
        int userLen = userAnswer.trim().split("\\s+").length;
        int modelLen = modelAnswer.trim().split("\\s+").length;
        if (modelLen == 0) return 1.0;
        double ratio = (double) userLen / modelLen;
        if (ratio >= 0.3 && ratio <= 2.0) return 1.0;
        if (ratio < 0.3) return Math.max(0.5, ratio / 0.3);
        return Math.max(0.7, 2.0 / ratio);
    }

    private Set<String> tokenize(String text) {
        return Arrays.stream(text.toLowerCase().split("[\\s\\p{Punct}]+"))
                .filter(t -> t.length() > 2)
                .collect(Collectors.toSet());
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
