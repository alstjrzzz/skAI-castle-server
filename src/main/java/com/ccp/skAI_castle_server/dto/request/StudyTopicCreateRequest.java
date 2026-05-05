package com.ccp.skAI_castle_server.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
@Schema(description = "Request body to create a new study topic")
public class StudyTopicCreateRequest {

    @NotBlank
    @Size(max = 255)
    @Schema(description = "Topic title", example = "Machine Learning Fundamentals")
    private String title;
}
