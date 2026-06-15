package com.ccp.skAI_castle_server.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.util.List;

@Getter
@Schema(description = "Batch evaluation request containing answers for all recall questions")
public class EvaluateRequest {

    @NotEmpty
    @Valid
    @Schema(description = "List of question-answer pairs")
    private List<UserAnswerItem> answers;

    @Getter
    @Schema(description = "A single question and the user's answer (free-form text or selected choice label)")
    public static class UserAnswerItem {

        @NotNull
        @Schema(description = "Evaluation question ID", example = "1")
        private Long questionId;

        @Schema(description = "User's free-form answer for WRITTEN questions. Null for MULTIPLE_CHOICE.",
                example = "Supervised learning is a type of ML where the model learns from labeled training data.")
        private String userAnswer;

        @Schema(description = "Selected choice label for MULTIPLE_CHOICE questions (A/B/C/D). Null for WRITTEN.",
                example = "A")
        private String selectedChoice;
    }
}
