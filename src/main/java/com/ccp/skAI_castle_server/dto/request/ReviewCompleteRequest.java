package com.ccp.skAI_castle_server.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "Request body to submit an answer for a spaced-repetition review card")
public class ReviewCompleteRequest {

    @Schema(description = "User's review answer for WRITTEN questions. Null for MULTIPLE_CHOICE.",
            example = "Backpropagation is an algorithm that computes gradients by applying the chain rule...")
    private String answer;

    @Schema(description = "Selected choice label for MULTIPLE_CHOICE questions (A/B/C/D). Null for WRITTEN.",
            example = "B")
    private String selectedChoice;
}
