package com.ccp.skAI_castle_server.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "AI tutor's response to the user's chat message")
public class ChatMessageResponse {

    @Schema(description = "AI tutor's reply message", example = "Supervised learning is a type of machine learning where the model learns from labeled training data...")
    private String message;

    @Schema(description = "Turn number of this exchange in the conversation", example = "3")
    private int turnNumber;
}
