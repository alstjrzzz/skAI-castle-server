package com.ccp.skAI_castle_server.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "Recall evaluation result with overall score, feedback, and per-question details including model answers")
public class EvaluationResultResponse {

    @Schema(description = "Evaluation ID", example = "1")
    private Long evaluationId;

    @Schema(description = "Overall score (0–100)", example = "75")
    private Integer score;

    @Schema(description = "AI-generated feedback text based on the score", example = "Good understanding overall. You correctly identified the key characteristics of supervised learning...")
    private String feedback;

    @Schema(description = "Per-question results with model answers now revealed")
    private List<QuestionResult> questions;

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "Individual question result with model answer revealed")
    public static class QuestionResult {

        @Schema(description = "Evaluation question ID", example = "1")
        private Long id;

        @Schema(description = "Display order", example = "1")
        private int questionOrder;

        @Schema(description = "Question text", example = "Explain what supervised learning is.")
        private String question;

        @Schema(description = "Model answer revealed after evaluation", example = "Supervised learning is a paradigm where a model is trained on labeled data...")
        private String modelAnswer;

        @Schema(description = "User's submitted answer", example = "Supervised learning uses labeled examples to train a model.")
        private String userAnswer;

        @Schema(description = "Next scheduled review date (SM-2 first interval applied)", example = "2024-01-06")
        private LocalDate nextReviewDate;
    }
}
