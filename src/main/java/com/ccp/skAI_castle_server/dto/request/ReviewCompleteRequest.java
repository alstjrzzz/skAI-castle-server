package com.ccp.skAI_castle_server.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
@Schema(description = "Request body to submit an answer for a spaced-repetition review card")
public class ReviewCompleteRequest {

    @NotBlank
    @Schema(description = "User's review answer", example = "Backpropagation is an algorithm that computes gradients by applying the chain rule...")
    private String answer;
}
