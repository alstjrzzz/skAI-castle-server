package com.ccp.skAI_castle_server.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "Result of finishing a session: the created evaluation and recall questions. Model answers are intentionally omitted.")
public class FinishSessionResponse {

    @Schema(description = "Created evaluation ID", example = "1")
    private Long evaluationId;

    @Schema(description = "AI-generated recall questions to answer")
    private List<QuestionItem> questions;

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "A recall question without the model answer")
    public static class QuestionItem {

        @Schema(description = "Evaluation question ID", example = "1")
        private Long id;

        @Schema(description = "Display order of this question", example = "1")
        private int questionOrder;

        @Schema(description = "Question text", example = "Explain what supervised learning is and provide a real-world example.")
        private String question;
    }
}
