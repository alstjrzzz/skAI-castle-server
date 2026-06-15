package com.ccp.skAI_castle_server.domain.entity;

import com.ccp.skAI_castle_server.domain.QuestionType;
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

    private Integer evaluationScore; // score from the initial recall evaluation

    private Integer lastScore; // score of the most recent review attempt

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private QuestionType questionType; // null treated as WRITTEN for backwards compatibility

    @Column(columnDefinition = "TEXT")
    private String choices; // JSON array for MC: [{"label":"A","text":"...","isCorrect":true}, ...]

    private Boolean isRecallQuestion; // null treated as true for backwards compatibility

    @Builder
    private EvaluationQuestion(Evaluation evaluation, Integer questionOrder, String question,
                                String modelAnswer, String userAnswer, LocalDate nextReviewDate,
                                String keywords, QuestionType questionType, String choices,
                                Boolean isRecallQuestion) {
        this.evaluation = evaluation;
        this.questionOrder = questionOrder;
        this.question = question;
        this.modelAnswer = modelAnswer;
        this.userAnswer = userAnswer;
        this.nextReviewDate = nextReviewDate;
        this.reviewCount = 0;
        this.keywords = keywords;
        this.questionType = questionType != null ? questionType : QuestionType.WRITTEN;
        this.choices = choices;
        this.isRecallQuestion = isRecallQuestion != null ? isRecallQuestion : Boolean.TRUE;
    }

    public void submitAnswer(String userAnswer, int evaluationScore) {
        this.userAnswer = userAnswer;
        this.evaluationScore = evaluationScore;
    }

    public void initReviewSchedule(LocalDate nextReviewDate) {
        this.nextReviewDate = nextReviewDate;
        this.ef = 2.5;
        this.lastInterval = 1;
    }

    public void applySmResult(LocalDate nextReviewDate, int reviewCount, double ef, int lastInterval, int score) {
        this.nextReviewDate = nextReviewDate;
        this.reviewCount = reviewCount;
        this.ef = ef;
        this.lastInterval = lastInterval;
        this.lastScore = score;
    }

    public boolean isRecall() {
        return isRecallQuestion == null || Boolean.TRUE.equals(isRecallQuestion);
    }
}
