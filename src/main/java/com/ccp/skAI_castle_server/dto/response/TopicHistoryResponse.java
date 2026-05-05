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
@Schema(description = "Dashboard view for a topic: outline, all sessions, and their evaluation and review status")
public class TopicHistoryResponse {

    @Schema(description = "Topic ID", example = "1")
    private Long topicId;

    @Schema(description = "Topic title", example = "Machine Learning Fundamentals")
    private String title;

    @Schema(description = "Topic status", example = "IN_PROGRESS", allowableValues = {"IN_PROGRESS", "COMPLETED", "ARCHIVED"})
    private String status;

    @Schema(description = "AI-generated outline. Null if not yet generated.")
    private OutlineDto outline;

    @Schema(description = "List of all chat sessions for this topic, newest first")
    private List<SessionHistoryItem> sessions;

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "A single session with its evaluation summary")
    public static class SessionHistoryItem {

        @Schema(description = "Session ID", example = "1")
        private Long sessionId;

        @Schema(description = "Session status", example = "CLOSED", allowableValues = {"ACTIVE", "CLOSED"})
        private String status;

        @Schema(description = "Session creation timestamp", example = "2024-01-01T09:00:00")
        private LocalDateTime createdAt;

        @Schema(description = "Evaluation summary for this session. Null if session has not been finished yet.")
        private EvaluationSummary evaluation;

        @Getter
        @Builder
        @AllArgsConstructor
        @Schema(description = "Evaluation summary including score and pending review count")
        public static class EvaluationSummary {

            @Schema(description = "Evaluation ID", example = "1")
            private Long evaluationId;

            @Schema(description = "Overall evaluation score (0–100). Null if answers have not been submitted yet.", example = "75")
            private Integer score;

            @Schema(description = "AI-generated feedback text. Null if not yet evaluated.", example = "Good understanding overall...")
            private String feedback;

            @Schema(description = "Total number of recall questions in this evaluation", example = "5")
            private int questionCount;

            @Schema(description = "Number of questions with a pending review scheduled for today or earlier", example = "2")
            private long pendingReviewCount;
        }
    }
}
