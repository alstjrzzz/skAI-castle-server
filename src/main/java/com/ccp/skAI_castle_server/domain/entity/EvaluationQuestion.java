package com.ccp.skAI_castle_server.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "evaluation_questions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EvaluationQuestion extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluation_id", nullable = false)
    private Evaluation evaluation;

    @Column(nullable = false)
    private Integer questionOrder;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String modelAnswer;

    @Column(columnDefinition = "TEXT")
    private String userAnswer;

    private LocalDate nextReviewDate;

    @Column(nullable = false)
    private Integer reviewCount;

    @Column(columnDefinition = "TEXT")
    private String keywords; // JSON array: ["keyword1", "keyword2"]

    private Double ef; // SM-2 ease factor (initialized on first review schedule)

    private Integer lastInterval; // SM-2 last interval in days

    @Builder
    private EvaluationQuestion(Evaluation evaluation, Integer questionOrder, String question,
                                String modelAnswer, String userAnswer, LocalDate nextReviewDate,
                                String keywords) {
        this.evaluation = evaluation;
        this.questionOrder = questionOrder;
        this.question = question;
        this.modelAnswer = modelAnswer;
        this.userAnswer = userAnswer;
        this.nextReviewDate = nextReviewDate;
        this.reviewCount = 0;
        this.keywords = keywords;
    }

    public void submitAnswer(String userAnswer) {
        this.userAnswer = userAnswer;
    }

    public void initReviewSchedule(LocalDate nextReviewDate) {
        this.nextReviewDate = nextReviewDate;
        this.ef = 2.5;
        this.lastInterval = 1;
    }

    public void applySmResult(LocalDate nextReviewDate, int reviewCount, double ef, int lastInterval) {
        this.nextReviewDate = nextReviewDate;
        this.reviewCount = reviewCount;
        this.ef = ef;
        this.lastInterval = lastInterval;
    }
}
