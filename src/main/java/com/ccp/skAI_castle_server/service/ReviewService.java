package com.ccp.skAI_castle_server.service;

import com.ccp.skAI_castle_server.domain.QuestionType;
import com.ccp.skAI_castle_server.domain.entity.EvaluationQuestion;
import com.ccp.skAI_castle_server.domain.entity.User;
import com.ccp.skAI_castle_server.dto.request.ReviewCompleteRequest;
import com.ccp.skAI_castle_server.dto.response.ChoiceItem;
import com.ccp.skAI_castle_server.dto.response.ReviewCardResponse;
import com.ccp.skAI_castle_server.dto.response.ReviewResultResponse;
import com.ccp.skAI_castle_server.exception.ApiException;
import com.ccp.skAI_castle_server.repository.EvaluationQuestionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.ccp.skAI_castle_server.dto.response.ApiResultCode.QUESTION_NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final EvaluationQuestionRepository evaluationQuestionRepository;
    private final ScoringService scoringService;
    private final Sm2Service sm2Service;
    private final ObjectMapper objectMapper;

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
                        .questionType(q.getQuestionType())
                        .choices(parseChoices(q.getChoices(), false))
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public ReviewResultResponse completeReview(Long questionId, ReviewCompleteRequest request, User user) {
        EvaluationQuestion question = evaluationQuestionRepository.findByIdAndEvaluationUser(questionId, user)
                .orElseThrow(() -> new ApiException(QUESTION_NOT_FOUND));

        int score;
        if (question.getQuestionType() == QuestionType.MULTIPLE_CHOICE) {
            String correctLabel = findCorrectLabel(question.getChoices());
            score = correctLabel.equals(request.getSelectedChoice()) ? 100 : 0;
        } else {
            score = scoringService.score(question.getQuestion(), request.getAnswer(), question.getModelAnswer(), question.getKeywords());
        }

        double ef = question.getEf() != null ? question.getEf() : 2.5;
        int lastInterval = question.getLastInterval() != null ? question.getLastInterval() : 1;

        Sm2Service.Sm2Result sm2 = sm2Service.compute(score, question.getReviewCount(), ef, lastInterval);
        question.applySmResult(sm2.nextReviewDate(), sm2.reviewCount(), sm2.ef(), sm2.lastInterval(), score);

        return ReviewResultResponse.builder()
                .score(score)
                .questionType(question.getQuestionType())
                .modelAnswer(question.getModelAnswer())
                .choices(parseChoices(question.getChoices(), true))
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
                .questionType(question.getQuestionType())
                .choices(parseChoices(question.getChoices(), false))
                .build();
    }

    @Transactional(readOnly = true)
    public ReviewResultResponse getReviewResult(Long questionId, User user) {
        EvaluationQuestion question = evaluationQuestionRepository.findByIdAndEvaluationUser(questionId, user)
                .orElseThrow(() -> new ApiException(QUESTION_NOT_FOUND));

        return ReviewResultResponse.builder()
                .score(question.getLastScore() != null ? question.getLastScore() : 0)
                .questionType(question.getQuestionType())
                .modelAnswer(question.getModelAnswer())
                .choices(parseChoices(question.getChoices(), true))
                .nextReviewDate(question.getNextReviewDate())
                .reviewCount(question.getReviewCount())
                .build();
    }

    private List<ChoiceItem> parseChoices(String choicesJson, boolean revealCorrect) {
        if (choicesJson == null) return null;
        try {
            JsonNode arr = objectMapper.readTree(choicesJson);
            List<ChoiceItem> list = new ArrayList<>();
            for (JsonNode n : arr) {
                Boolean isCorrect = revealCorrect ? n.path("isCorrect").asBoolean() : null;
                list.add(ChoiceItem.builder()
                        .label(n.path("label").asText())
                        .text(n.path("text").asText())
                        .isCorrect(isCorrect)
                        .build());
            }
            return list;
        } catch (Exception e) {
            log.warn("Failed to parse choices JSON", e);
            return null;
        }
    }

    private String findCorrectLabel(String choicesJson) {
        if (choicesJson == null) return "";
        try {
            JsonNode arr = objectMapper.readTree(choicesJson);
            for (JsonNode n : arr) {
                if (n.path("isCorrect").asBoolean()) {
                    return n.path("label").asText();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to find correct label in choices JSON", e);
        }
        return "";
    }
}
