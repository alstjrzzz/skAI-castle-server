package com.ccp.skAI_castle_server.service;

import com.ccp.skAI_castle_server.domain.entity.EvaluationQuestion;
import com.ccp.skAI_castle_server.domain.entity.User;
import com.ccp.skAI_castle_server.dto.response.ReviewCardResponse;
import com.ccp.skAI_castle_server.dto.response.ReviewResultResponse;
import com.ccp.skAI_castle_server.exception.ApiException;
import com.ccp.skAI_castle_server.repository.EvaluationQuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import static com.ccp.skAI_castle_server.dto.response.ApiResultCode.QUESTION_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final EvaluationQuestionRepository evaluationQuestionRepository;
    private final ScoringService scoringService;
    private final Sm2Service sm2Service;

    @Transactional(readOnly = true)
    public List<ReviewCardResponse> getTodayReviews(User user) {
        LocalDate today = LocalDate.now();
        List<EvaluationQuestion> due = evaluationQuestionRepository.findDueForReview(user, today);

        return due.stream()
                .map(q -> ReviewCardResponse.builder()
                        .questionId(q.getId())
                        .question(q.getQuestion())
                        .topicTitle(q.getEvaluation().getStudyTopic().getTitle())
                        .reviewCount(q.getReviewCount())
                        .reviewDate(q.getNextReviewDate())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public ReviewResultResponse completeReview(Long questionId, String userAnswer, User user) {
        EvaluationQuestion question = evaluationQuestionRepository.findByIdAndEvaluationUser(questionId, user)
                .orElseThrow(() -> new ApiException(QUESTION_NOT_FOUND));

        int score = scoringService.score(question.getQuestion(), userAnswer, question.getModelAnswer(), question.getKeywords());

        double ef = question.getEf() != null ? question.getEf() : 2.5;
        int lastInterval = question.getLastInterval() != null ? question.getLastInterval() : 1;

        Sm2Service.Sm2Result sm2 = sm2Service.compute(score, question.getReviewCount(), ef, lastInterval);

        question.applySmResult(sm2.nextReviewDate(), sm2.reviewCount(), sm2.ef(), sm2.lastInterval(), score);

        return ReviewResultResponse.builder()
                .score(score)
                .modelAnswer(question.getModelAnswer())
                .nextReviewDate(sm2.nextReviewDate())
                .reviewCount(sm2.reviewCount())
                .build();
    }

    @Transactional(readOnly = true)
    public ReviewCardResponse getReviewCard(Long questionId, User user) {
        EvaluationQuestion question = evaluationQuestionRepository.findByIdAndEvaluationUser(questionId, user)
                .orElseThrow(() -> new ApiException(QUESTION_NOT_FOUND));

        return ReviewCardResponse.builder()
                .questionId(question.getId())
                .question(question.getQuestion())
                .topicTitle(question.getEvaluation().getStudyTopic().getTitle())
                .reviewCount(question.getReviewCount())
                .reviewDate(question.getNextReviewDate())
                .build();
    }

    @Transactional(readOnly = true)
    public ReviewResultResponse getReviewResult(Long questionId, User user) {
        EvaluationQuestion question = evaluationQuestionRepository.findByIdAndEvaluationUser(questionId, user)
                .orElseThrow(() -> new ApiException(QUESTION_NOT_FOUND));

        return ReviewResultResponse.builder()
                .score(question.getLastScore() != null ? question.getLastScore() : 0)
                .modelAnswer(question.getModelAnswer())
                .nextReviewDate(question.getNextReviewDate())
                .reviewCount(question.getReviewCount())
                .build();
    }
}
