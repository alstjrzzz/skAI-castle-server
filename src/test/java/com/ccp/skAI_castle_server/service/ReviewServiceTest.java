package com.ccp.skAI_castle_server.service;

import com.ccp.skAI_castle_server.domain.UserRole;
import com.ccp.skAI_castle_server.domain.entity.*;
import com.ccp.skAI_castle_server.dto.response.ReviewCardResponse;
import com.ccp.skAI_castle_server.dto.response.ReviewResultResponse;
import com.ccp.skAI_castle_server.exception.ApiException;
import com.ccp.skAI_castle_server.repository.EvaluationQuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.ccp.skAI_castle_server.dto.response.ApiResultCode.QUESTION_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock EvaluationQuestionRepository evaluationQuestionRepository;
    @Mock ScoringService scoringService;
    @Mock Sm2Service sm2Service;

    private ReviewService reviewService;
    private User testUser;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewService(evaluationQuestionRepository, scoringService, sm2Service);
        testUser = User.builder()
                .email("test@example.com").password("encoded")
                .nickname("Test").role(UserRole.USER).build();
    }

    @Test
    void getTodayReviews_returnsMappedCardList() {
        EvaluationQuestion question = mock(EvaluationQuestion.class);
        Evaluation evaluation = mock(Evaluation.class);
        StudyTopic topic = mock(StudyTopic.class);

        given(question.getId()).willReturn(1L);
        given(question.getQuestion()).willReturn("What is overfitting?");
        given(question.getEvaluation()).willReturn(evaluation);
        given(question.getReviewCount()).willReturn(2);
        given(question.getNextReviewDate()).willReturn(LocalDate.now());
        given(evaluation.getStudyTopic()).willReturn(topic);
        given(topic.getTitle()).willReturn("ML Fundamentals");

        given(evaluationQuestionRepository.findDueForReview(testUser, LocalDate.now()))
                .willReturn(List.of(question));

        List<ReviewCardResponse> cards = reviewService.getTodayReviews(testUser);

        assertThat(cards).hasSize(1);
        assertThat(cards.get(0).getQuestion()).isEqualTo("What is overfitting?");
        assertThat(cards.get(0).getTopicTitle()).isEqualTo("ML Fundamentals");
        assertThat(cards.get(0).getReviewCount()).isEqualTo(2);
    }

    @Test
    void getTodayReviews_noCards_returnsEmptyList() {
        given(evaluationQuestionRepository.findDueForReview(testUser, LocalDate.now()))
                .willReturn(List.of());

        assertThat(reviewService.getTodayReviews(testUser)).isEmpty();
    }

    @Test
    void completeReview_success_appliesSm2AndReturnsResult() {
        EvaluationQuestion question = EvaluationQuestion.builder()
                .evaluation(mock(Evaluation.class))
                .questionOrder(1)
                .question("What is gradient descent?")
                .modelAnswer("An optimization algorithm that minimizes loss.")
                .keywords("[\"gradient\",\"optimization\",\"loss\"]")
                .build();
        ReflectionTestUtils.setField(question, "id", 1L);
        question.initReviewSchedule(LocalDate.now());

        given(evaluationQuestionRepository.findByIdAndEvaluationUser(1L, testUser))
                .willReturn(Optional.of(question));
        given(scoringService.score(anyString(), anyString(), anyString(), anyString())).willReturn(75);
        given(sm2Service.compute(anyInt(), anyInt(), any(Double.class), anyInt()))
                .willReturn(new Sm2Service.Sm2Result(LocalDate.now().plusDays(6), 2, 2.5, 6));

        ReviewResultResponse response = reviewService.completeReview(1L, "gradient descent optimizes the loss", testUser);

        assertThat(response.getScore()).isEqualTo(75);
        assertThat(response.getReviewCount()).isEqualTo(2);
        assertThat(response.getNextReviewDate()).isEqualTo(LocalDate.now().plusDays(6));
        assertThat(response.getModelAnswer()).isNotBlank();
    }

    @Test
    void completeReview_questionNotFound_throwsApiException() {
        given(evaluationQuestionRepository.findByIdAndEvaluationUser(99L, testUser))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.completeReview(99L, "answer", testUser))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getResultCode()).isEqualTo(QUESTION_NOT_FOUND));
    }
}
