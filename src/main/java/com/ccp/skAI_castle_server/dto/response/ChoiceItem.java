package com.ccp.skAI_castle_server.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "A choice option for a multiple-choice question")
public class ChoiceItem {

    @Schema(description = "Choice label (A/B/C/D)", example = "A")
    private String label;

    @Schema(description = "Choice text", example = "The algorithm minimizes a loss function using gradients.")
    private String text;

    @Schema(description = "Whether this choice is correct. Null before answering, true/false after grading.", example = "true")
    private Boolean isCorrect;
}
