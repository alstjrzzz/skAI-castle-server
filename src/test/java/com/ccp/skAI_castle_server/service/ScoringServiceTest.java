package com.ccp.skAI_castle_server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ScoringServiceTest {

    @Mock
    private LlmService llmService;

    private ScoringService scoringService;

    @BeforeEach
    void setUp() {
        scoringService = new ScoringService(llmService, new ObjectMapper());
    }

    @Test
    void score_identicalEmbeddings_highScore() {
        double[] emb = {0.6, 0.5, 0.4, 0.3};
        given(llmService.getEmbeddings(anyList())).willReturn(List.of(emb, emb));

        String answer = "supervised learning trains on labeled data";
        int score = scoringService.score(answer, answer, "[\"supervised\",\"labeled\",\"data\"]");

        assertThat(score).isGreaterThan(80);
    }

    @Test
    void score_noEmbeddings_fallsBackToSparseAndKeyword() {
        given(llmService.getEmbeddings(anyList())).willReturn(List.of(new double[0], new double[0]));

        String answer = "machine learning uses data to train models";
        int score = scoringService.score(answer, answer, "[\"machine\",\"learning\",\"data\"]");

        assertThat(score).isGreaterThan(50);
    }

    @Test
    void score_completelyIrrelevantAnswer_lowScore() {
        given(llmService.getEmbeddings(anyList())).willReturn(List.of(new double[0], new double[0]));

        int score = scoringService.score(
                "pizza is delicious food",
                "supervised learning uses labeled training data to build predictive models",
                "[\"supervised\",\"labeled\",\"training\",\"predictive\"]"
        );

        assertThat(score).isLessThan(30);
    }

    @Test
    void score_veryShortAnswer_penalizedByLengthFactor() {
        given(llmService.getEmbeddings(anyList())).willReturn(List.of(new double[0], new double[0]));

        // userAnswer is only 1 word vs model answer of many words → ratio < 0.3 → penalty
        int score = scoringService.score(
                "yes",
                "supervised learning is a machine learning paradigm where the model learns from labeled examples",
                "[\"supervised\",\"labeled\"]"
        );

        assertThat(score).isLessThan(40);
    }

    @Test
    void score_alwaysBetween0And100() {
        given(llmService.getEmbeddings(anyList())).willReturn(List.of(new double[0], new double[0]));

        int score1 = scoringService.score("", "model answer text", "[]");
        int score2 = scoringService.score("perfect answer with all keywords", "perfect answer with all keywords",
                "[\"perfect\",\"answer\",\"keywords\"]");

        assertThat(score1).isBetween(0, 100);
        assertThat(score2).isBetween(0, 100);
    }

    @Test
    void score_emptyKeywords_defaultsToHalfCredit() {
        given(llmService.getEmbeddings(anyList())).willReturn(List.of(new double[0], new double[0]));

        // With no keywords, keywordMatchRatio returns 0.5 by default
        int score = scoringService.score("some answer text here", "some answer text here", "[]");

        // Bag-of-words similarity should be 1.0 (identical), keyword defaults to 0.5 → decent score
        assertThat(score).isGreaterThan(40);
    }
}
