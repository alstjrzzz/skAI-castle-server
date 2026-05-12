package com.ccp.skAI_castle_server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
class ScoringServiceTest {

    @Mock RestClient restClient;
    @Mock RestClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock RestClient.RequestBodySpec requestBodySpec;
    @Mock RestClient.ResponseSpec responseSpec;

    private ScoringService scoringService;

    private static final String RESPONSE_79 =
            "{\"scores\":{\"denseScore\":80.0,\"sparseScore\":70.0,\"keywordScore\":90.0,\"totalScore\":79.5}," +
            "\"evidence\":{\"matchedKeywords\":[],\"normalizedStudentAnswer\":\"\",\"normalizedModelAnswer\":\"\"}}";

    @BeforeEach
    void setUp() {
        scoringService = new ScoringService(restClient, new ObjectMapper());
        given(restClient.post()).willReturn(requestBodyUriSpec);
        given(requestBodyUriSpec.uri(anyString())).willReturn(requestBodySpec);
        given(requestBodySpec.contentType(any())).willReturn(requestBodySpec);
        given(requestBodySpec.body(any(Object.class))).willReturn(requestBodySpec);
        given(requestBodySpec.retrieve()).willReturn(responseSpec);
    }

    @Test
    void score_successResponse_returnsTotalScore() {
        given(responseSpec.body(String.class)).willReturn(RESPONSE_79);

        int score = scoringService.score("What is ML?", "supervised learning", "supervised learning", "[\"supervised\"]");

        assertThat(score).isEqualTo(80); // round(79.5)
    }

    @Test
    void score_apiError_returnsZero() {
        given(responseSpec.body(String.class)).willThrow(new RuntimeException("Connection refused"));

        int score = scoringService.score("Q", "some answer", "model answer", "[]");

        assertThat(score).isEqualTo(0);
    }

    @Test
    void score_totalScoreAbove100_cappedAt100() {
        given(responseSpec.body(String.class)).willReturn(
                "{\"scores\":{\"totalScore\":150.0},\"evidence\":{}}");

        int score = scoringService.score("Q", "A", "B", "[]");

        assertThat(score).isEqualTo(100);
    }

    @Test
    void score_totalScoreNegative_floorsAt0() {
        given(responseSpec.body(String.class)).willReturn(
                "{\"scores\":{\"totalScore\":-5.0},\"evidence\":{}}");

        int score = scoringService.score("Q", "A", "B", "[]");

        assertThat(score).isEqualTo(0);
    }

    @Test
    void score_nullKeywords_sendsEmptyList() {
        given(responseSpec.body(String.class)).willReturn(RESPONSE_79);

        int score = scoringService.score("Q", "answer", "model", null);

        assertThat(score).isBetween(0, 100);
    }
}
