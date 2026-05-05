package com.ccp.skAI_castle_server.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI-generated topic outline")
public class OutlineDto {

    @Schema(description = "List of chapters in the outline")
    private List<ChapterDto> chapters;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "A single chapter with title and key concepts")
    public static class ChapterDto {

        @Schema(description = "Chapter title", example = "Introduction to Supervised Learning")
        private String title;

        @Schema(description = "Key concepts covered in this chapter",
                example = "[\"classification\", \"regression\", \"training data\", \"labels\"]")
        private List<String> keywords;
    }
}
