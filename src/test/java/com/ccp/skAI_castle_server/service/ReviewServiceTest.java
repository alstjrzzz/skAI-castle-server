package com.ccp.skAI_castle_server.service;

import com.ccp.skAI_castle_server.domain.UserRole;
import com.ccp.skAI_castle_server.domain.entity.*;
import com.ccp.skAI_castle_server.dto.request.ReviewCompleteRequest;
import com.ccp.skAI_castle_server.dto.response.ReviewCardResponse;
import com.ccp.skAI_castle_server.dto.response.ReviewResultResponse;
import com.ccp.skAI_castle_server.exception.ApiException;
import com.ccp.skAI_castle_server.repository.EvaluationQuestionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
        reviewService = new ReviewService(evaluationQuestionRepository, scoringService, sm2Service, new ObjectMapper());
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
        given(question.getQuestionType()).willReturn(null);
        given(question.getChoices()).willReturn(null);
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
    void completeReview_writtenQuestion_appliesSm2AndReturnsResult() {
        EvaluationQuestion question = EvaluationQuestion.builder()
                .evaluation(mock(Evaluation.class))
                .questionOrder(1)
                .question("What is gradient descent?")
                .modelAnswer("An optimization algorithm that minimizes loss.")
                .keywords("[\"gradient\",\"optimization\",\"loss\"]")
                .build();
        ReflectionTestUtils.setField(question, "id", 1L);
        question.initReviewSchedule(LocalDate.now());

        ReviewCompleteRequest request = mock(ReviewCompleteRequest.class);
        given(request.getAnswer()).willReturn("gradient descent optimizes the loss");

        given(evaluationQuestionRepository.findByIdAndEvaluationUser(1L, testUser))
                .willReturn(Optional.of(question));
        given(scoringService.score(anyString(), anyString(), anyString(), anyString())).willReturn(75);
        given(sm2Service.compute(anyInt(), anyInt(), any(Double.class), anyInt()))
                .willReturn(new Sm2Service.Sm2Result(LocalDate.now().plusDays(6), 2, 2.5, 6));

        ReviewResultResponse response = reviewService.completeReview(1L, request, testUser);

        assertThat(response.getScore()).isEqualTo(75);
        assertThat(response.getReviewCount()).isEqualTo(2);
        assertThat(response.getNextReviewDate()).isEqualTo(LocalDate.now().plusDays(6));
        assertThat(response.getModelAnswer()).isNotBlank();
    }

    @Test
    void completeReview_questionNotFound_throwsApiException() {
        given(evaluationQuestionRepository.findByIdAndEvaluationUser(99L, testUser))
                .willReturn(Optional.empty());

        ReviewCompleteRequest request = mock(ReviewCompleteRequest.class);
        assertThatThrownBy(() -> reviewService.completeReview(99L, request, testUser))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getResultCode()).isEqualTo(QUESTION_NOT_FOUND));
    }

    @Test
    void getReviewCard_success_returnsMappedCard() {
        EvaluationQuestion question = mock(EvaluationQuestion.class);
        Evaluation evaluation = mock(Evaluation.class);
        StudyTopic topic = mock(StudyTopic.class);

        given(question.getId()).willReturn(1L);
        given(question.getQuestion()).willReturn("What is gradient descent?");
        given(question.getEvaluation()).willReturn(evaluation);
        given(question.getReviewCount()).willReturn(3);
        given(question.getNextReviewDate()).willReturn(LocalDate.now());
        given(question.getQuestionType()).willReturn(null);
        given(question.getChoices()).willReturn(null);
        given(evaluation.getStudyTopic()).willReturn(topic);
        given(topic.getTitle()).willReturn("ML Fundamentals");
        given(evaluationQuestionRepository.findByIdAndEvaluationUser(1L, testUser))
                .willReturn(Optional.of(question));

        ReviewCardResponse card = reviewService.getReviewCard(1L, testUser);

        assertThat(card.getQuestionId()).isEqualTo(1L);
        assertThat(card.getQuestion()).isEqualTo("What is gradient descent?");
        assertThat(card.getTopicTitle()).isEqualTo("ML Fundamentals");
        assertThat(card.getReviewCount()).isEqualTo(3);
    }

    @Test
    void getReviewCard_notFound_throwsApiException() {
        given(evaluationQuestionRepository.findByIdAndEvaluationUser(99L, testUser))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.getReviewCard(99L, testUser))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getResultCode()).isEqualTo(QUESTION_NOT_FOUND));
    }

    @Test
    void getReviewResult_success_returnsStoredScore() {
        EvaluationQuestion question = EvaluationQuestion.builder()
                .evaluation(mock(Evaluation.class))
                .questionOrder(1)
                .question("What is gradient descent?")
                .modelAnswer("An optimization algorithm that minimizes loss.")
                .keywords("[\"gradient\",\"optimization\"]")
                .build();
        question.initReviewSchedule(LocalDate.now().plusDays(1));
        question.applySmResult(LocalDate.now().plusDays(6), 2, 2.5, 6, 80);

        given(evaluationQuestionRepository.findByIdAndEvaluationUser(1L, testUser))
                .willReturn(Optional.of(question));

        ReviewResultResponse result = reviewService.getReviewResult(1L, testUser);

        assertThat(result.getScore()).isEqualTo(80);
        assertThat(result.getReviewCount()).isEqualTo(2);
        assertThat(result.getModelAnswer()).isEqualTo("An optimization algorithm that minimizes loss.");
    }

    @Test
    void getReviewResult_notFound_throwsApiException() {
        given(evaluationQuestionRepository.findByIdAndEvaluationUser(99L, testUser))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.getReviewResult(99L, testUser))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getResultCode()).isEqualTo(QUESTION_NOT_FOUND));
    }
}
