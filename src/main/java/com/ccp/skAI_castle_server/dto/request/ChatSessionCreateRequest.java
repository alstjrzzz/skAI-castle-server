package com.ccp.skAI_castle_server.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
@Schema(description = "Request body to start a new chat session for a study topic")
public class ChatSessionCreateRequest {

    @NotNull
    @Schema(description = "ID of the study topic for this session", example = "1")
    private Long topicId;
}
