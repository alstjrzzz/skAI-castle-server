package com.ccp.skAI_castle_server.repository;

import com.ccp.skAI_castle_server.domain.entity.Evaluation;
import com.ccp.skAI_castle_server.domain.entity.EvaluationQuestion;
import com.ccp.skAI_castle_server.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EvaluationQuestionRepository extends JpaRepository<EvaluationQuestion, Long> {

    List<EvaluationQuestion> findByEvaluationOrderByQuestionOrderAsc(Evaluation evaluation);

    long countByEvaluation(Evaluation evaluation);

    @Query("SELECT COUNT(eq) FROM EvaluationQuestion eq WHERE eq.evaluation = :evaluation " +
           "AND eq.nextReviewDate IS NOT NULL AND eq.nextReviewDate <= :today")
    long countPendingReviews(@Param("evaluation") Evaluation evaluation, @Param("today") LocalDate today);

    @Query("SELECT eq FROM EvaluationQuestion eq WHERE eq.evaluation.user = :user " +
           "AND eq.nextReviewDate IS NOT NULL AND eq.nextReviewDate <= :today")
    List<EvaluationQuestion> findDueForReview(@Param("user") User user, @Param("today") LocalDate today);

    Optional<EvaluationQuestion> findByIdAndEvaluationUser(Long id, User user);
}
