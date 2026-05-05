package com.ccp.skAI_castle_server.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "Study topic summary for list view")
public class StudyTopicSummaryResponse {

    @Schema(description = "Topic ID", example = "1")
    private Long id;

    @Schema(description = "Topic title", example = "Machine Learning Fundamentals")
    private String title;

    @Schema(description = "Topic status", example = "IN_PROGRESS", allowableValues = {"IN_PROGRESS", "COMPLETED", "ARCHIVED"})
    private String status;

    @Schema(description = "Whether an AI-generated outline exists", example = "true")
    private boolean hasOutline;

    @Schema(description = "Creation timestamp", example = "2024-01-01T09:00:00")
    private LocalDateTime createdAt;
}
