package com.ccp.skAI_castle_server.api;

import com.ccp.skAI_castle_server.dto.request.StudyTopicCreateRequest;
import com.ccp.skAI_castle_server.dto.response.*;
import com.ccp.skAI_castle_server.security.PrincipalDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Study Topic", description = "Study topic management and AI outline generation")
@RequestMapping("/api/topics")
@SecurityRequirement(name = "BearerAuth")
public interface StudyTopicApi {

    @Operation(
            summary = "Create study topic",
            description = "Creates a new study topic. Outline is not generated here — call POST /api/topics/{topicId}/outline separately."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Topic created successfully.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StudyTopicResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "code": 1001,
                                      "message": "Resource created successfully.",
                                      "success": true,
                                      "data": {
                                        "id": 1,
                                        "title": "Machine Learning Fundamentals",
                                        "outline": null,
                                        "status": "IN_PROGRESS",
                                        "createdAt": "2024-01-01T09:00:00",
                                        "updatedAt": "2024-01-01T09:00:00"
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid input.",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "code": 2001,
                                      "message": "Invalid input value.",
                                      "success": false,
                                      "data": null
                                    }
                                    """))
            )
    })
    @PostMapping
    ResponseEntity<ApiResponse<StudyTopicResponse>> createTopic(
            @Valid @RequestBody StudyTopicCreateRequest request,
            @AuthenticationPrincipal PrincipalDetails principal
    );

    @Operation(
            summary = "List my study topics",
            description = "Returns all study topics owned by the authenticated user, newest first."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Topic list retrieved successfully.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "code": 1000,
                                      "message": "Request processed successfully.",
                                      "success": true,
                                      "data": [
                                        {
                                          "id": 1,
                                          "title": "Machine Learning Fundamentals",
                                          "status": "IN_PROGRESS",
                                          "hasOutline": true,
                                          "createdAt": "2024-01-01T09:00:00"
                                        }
                                      ]
                                    }
                                    """)
                    )
            )
    })
    @GetMapping
    ResponseEntity<ApiResponse<List<StudyTopicSummaryResponse>>> getMyTopics(
            @AuthenticationPrincipal PrincipalDetails principal
    );

    @Operation(
            summary = "Get study topic detail",
            description = "Returns full topic details including the outline if it has been generated."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Topic retrieved successfully.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "code": 1000,
                                      "message": "Request processed successfully.",
                                      "success": true,
                                      "data": {
                                        "id": 1,
                                        "title": "Machine Learning Fundamentals",
                                        "outline": {
                                          "chapters": [
                                            { "title": "Introduction", "keywords": ["supervised", "unsupervised"] }
                                          ]
                                        },
                                        "status": "IN_PROGRESS",
                                        "createdAt": "2024-01-01T09:00:00",
                                        "updatedAt": "2024-01-01T09:05:00"
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Topic not found.",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "code": 4003,
                                      "message": "Study topic not found.",
                                      "success": false,
                                      "data": null
                                    }
                                    """))
            )
    })
    @GetMapping("/{topicId}")
    ResponseEntity<ApiResponse<StudyTopicResponse>> getTopic(
            @Parameter(description = "Study topic ID", example = "1") @PathVariable Long topicId,
            @AuthenticationPrincipal PrincipalDetails principal
    );

    @Operation(
            summary = "Generate topic outline (LLM)",
            description = "Calls the LLM to generate a structured outline (chapters + keywords) for the topic and saves it. " +
                    "Returns 409 if an outline already exists."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Outline generated and saved successfully.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "code": 1000,
                                      "message": "Request processed successfully.",
                                      "success": true,
                                      "data": {
                                        "chapters": [
                                          {
                                            "title": "Introduction to Supervised Learning",
                                            "keywords": ["classification", "regression", "training data", "labels"]
                                          },
                                          {
                                            "title": "Model Evaluation",
                                            "keywords": ["accuracy", "precision", "recall", "cross-validation"]
                                          }
                                        ]
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Topic not found.",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "code": 4003,
                                      "message": "Study topic not found.",
                                      "success": false,
                                      "data": null
                                    }
                                    """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Outline already exists.",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "code": 2010,
                                      "message": "Outline already exists for this topic.",
                                      "success": false,
                                      "data": null
                                    }
                                    """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "502",
                    description = "LLM API call failed.",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "code": 5002,
                                      "message": "External API server error.",
                                      "success": false,
                                      "data": null
                                    }
                                    """))
            )
    })
    @PostMapping("/{topicId}/outline")
    ResponseEntity<ApiResponse<OutlineDto>> generateOutline(
            @Parameter(description = "Study topic ID", example = "1") @PathVariable Long topicId,
            @AuthenticationPrincipal PrincipalDetails principal
    );

    @Operation(
            summary = "Get topic history (Dashboard)",
            description = "Returns a comprehensive dashboard view: topic details, all sessions, and their evaluation scores and pending review counts."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Topic history retrieved successfully.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "code": 1000,
                                      "message": "Request processed successfully.",
                                      "success": true,
                                      "data": {
                                        "topicId": 1,
                                        "title": "Machine Learning Fundamentals",
                                        "status": "IN_PROGRESS",
                                        "outline": { "chapters": [] },
                                        "sessions": [
                                          {
                                            "sessionId": 1,
                                            "status": "CLOSED",
                                            "createdAt": "2024-01-01T09:00:00",
                                            "evaluation": {
                                              "evaluationId": 1,
                                              "score": 75,
                                              "feedback": "Good understanding overall...",
                                              "questionCount": 5,
                                              "pendingReviewCount": 2
                                            }
                                          }
                                        ]
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Topic not found.",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "code": 4003,
                                      "message": "Study topic not found.",
                                      "success": false,
                                      "data": null
                                    }
                                    """))
            )
    })
    @GetMapping("/{topicId}/history")
    ResponseEntity<ApiResponse<TopicHistoryResponse>> getTopicHistory(
            @Parameter(description = "Study topic ID", example = "1") @PathVariable Long topicId,
            @AuthenticationPrincipal PrincipalDetails principal
    );
}
