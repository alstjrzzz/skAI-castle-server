package com.ccp.skAI_castle_server.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "Chat session details with full message history")
public class ChatSessionResponse {

    @Schema(description = "Session ID", example = "1")
    private Long id;

    @Schema(description = "Study topic ID this session belongs to", example = "1")
    private Long topicId;

    @Schema(description = "Study topic title", example = "Machine Learning Fundamentals")
    private String topicTitle;

    @Schema(description = "Session status", example = "ACTIVE", allowableValues = {"ACTIVE", "CLOSED"})
    private String status;

    @Schema(description = "All chat messages in this session, ordered by turn number")
    private List<MessageItem> messages;

    @Schema(description = "Session creation timestamp", example = "2024-01-01T09:00:00")
    private LocalDateTime createdAt;

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "A single chat message")
    public static class MessageItem {

        @Schema(description = "Message ID", example = "1")
        private Long id;

        @Schema(description = "Message role", example = "USER", allowableValues = {"USER", "ASSISTANT", "SYSTEM"})
        private String role;

        @Schema(description = "Message content", example = "Can you explain supervised learning?")
        private String content;

        @Schema(description = "Turn number in the conversation", example = "1")
        private int turnNumber;

        @Schema(description = "Message creation timestamp", example = "2024-01-01T09:00:00")
        private LocalDateTime createdAt;
    }
}
