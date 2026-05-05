package com.ccp.skAI_castle_server.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "A spaced-repetition review card for today. Model answer is intentionally omitted.")
public class ReviewCardResponse {

    @Schema(description = "Evaluation question ID (used as the review identifier)", example = "1")
    private Long questionId;

    @Schema(description = "Question text to review", example = "Explain what supervised learning is.")
    private String question;

    @Schema(description = "Title of the study topic this question belongs to", example = "Machine Learning Fundamentals")
    private String topicTitle;

    @Schema(description = "Number of times this card has been reviewed (SM-2 n)", example = "2")
    private int reviewCount;

    @Schema(description = "Scheduled review date", example = "2024-01-05")
    private LocalDate reviewDate;
}
