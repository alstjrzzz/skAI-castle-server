package com.ccp.skAI_castle_server.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "A user notification, typically sent when a review is due")
public class NotificationResponse {

    @Schema(description = "Notification ID", example = "1")
    private Long id;

    @Schema(description = "Notification message text", example = "It's time to review 'Machine Learning Fundamentals'. You have 3 cards due today.")
    private String message;

    @Schema(description = "Optional navigation link related to this notification", example = "/reviews/today")
    private String link;

    @Schema(description = "Whether the notification has been read", example = "false")
    private boolean isRead;

    @Schema(description = "Notification creation timestamp", example = "2024-01-05T09:00:00")
    private LocalDateTime createdAt;
}
