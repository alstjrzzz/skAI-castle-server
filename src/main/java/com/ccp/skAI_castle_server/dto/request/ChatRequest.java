package com.ccp.skAI_castle_server.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
@Schema(description = "User's chat message to the AI tutor")
public class ChatRequest {

    @NotBlank
    @Schema(description = "User's message content", example = "Can you explain what supervised learning is?")
    private String message;
}
