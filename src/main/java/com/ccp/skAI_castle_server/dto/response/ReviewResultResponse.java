package com.ccp.skAI_castle_server.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "Result of completing a review card, including score, revealed model answer, and next review date")
public class ReviewResultResponse {

    @Schema(description = "Score for this review attempt (0–100)", example = "85")
    private int score;

    @Schema(description = "Model answer revealed after submitting the review", example = "Supervised learning is a paradigm where a model is trained on labeled data...")
    private String modelAnswer;

    @Schema(description = "Next review date calculated by the SM-2 algorithm. Null if the user chose to stop reviewing this card.", example = "2024-01-12")
    private LocalDate nextReviewDate;

    @Schema(description = "Updated review count (SM-2 n)", example = "3")
    private int reviewCount;
}
