package com.ccp.skAI_castle_server.api;

import com.ccp.skAI_castle_server.dto.request.ChatRequest;
import com.ccp.skAI_castle_server.dto.request.ChatSessionCreateRequest;
import com.ccp.skAI_castle_server.dto.request.EvaluateRequest;
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

@Tag(name = "Tutoring", description = "AI tutoring sessions: start, chat, finish, and recall evaluation")
@RequestMapping("/sessions")
@SecurityRequirement(name = "BearerAuth")
public interface TutoringApi {

    @Operation(
            summary = "Start a new tutoring session",
            description = "Creates a new ACTIVE chat session linked to the specified study topic."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Session created successfully.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "code": 1001,
                                      "message": "Resource created successfully.",
                                      "success": true,
                                      "data": {
                                        "id": 1,
                                        "topicId": 1,
                                        "topicTitle": "Machine Learning Fundamentals",
                                        "status": "ACTIVE",
                                        "messages": [],
                                        "createdAt": "2024-01-01T09:00:00"
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Study topic not found.",
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
    @PostMapping
    ResponseEntity<ApiResponse<ChatSessionResponse>> createSession(
            @Valid @RequestBody ChatSessionCreateRequest request,
            @AuthenticationPrincipal PrincipalDetails principal
    );

    @Operation(
            summary = "Get session with full message history",
            description = "Returns session details and all chat messages ordered by turn number. " +
                    "Use this to restore the chat UI when returning to an in-progress session."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Session retrieved successfully.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "code": 1000,
                                      "message": "Request processed successfully.",
                                      "success": true,
                                      "data": {
                                        "id": 1,
                                        "topicId": 1,
                                        "topicTitle": "Machine Learning Fundamentals",
                                        "status": "ACTIVE",
                                        "messages": [
                                          {
                                            "id": 1,
                                            "role": "USER",
                                            "content": "What is supervised learning?",
                                            "turnNumber": 1,
                                            "createdAt": "2024-01-01T09:01:00"
                                          },
                                          {
                                            "id": 2,
                                            "role": "ASSISTANT",
                                            "content": "Supervised learning is...",
                                            "turnNumber": 1,
                                            "createdAt": "2024-01-01T09:01:05"
                                          }
                                        ],
                                        "createdAt": "2024-01-01T09:00:00"
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Session not found.",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "code": 4004,
                                      "message": "Chat session not found.",
                                      "success": false,
                                      "data": null
                                    }
                                    """))
            )
    })
    @GetMapping("/{sessionId}")
    ResponseEntity<ApiResponse<ChatSessionResponse>> getSession(
            @Parameter(description = "Chat session ID", example = "1") @PathVariable Long sessionId,
            @AuthenticationPrincipal PrincipalDetails principal
    );

    @Operation(
            summary = "Send a chat message (LLM)",
            description = "Sends the user's message to the AI tutor. " +
                    "The entire conversation history is loaded from DB and forwarded to the LLM each turn (stateless server). " +
                    "Returns only the AI's response message."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "AI response returned successfully.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ChatMessageResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "code": 1000,
                                      "message": "Request processed successfully.",
                                      "success": true,
                                      "data": {
                                        "message": "Supervised learning is a type of machine learning where the algorithm learns from labeled training data...",
                                        "turnNumber": 1
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Session not found.",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "code": 4004,
                                      "message": "Chat session not found.",
                                      "success": false,
                                      "data": null
                                    }
                                    """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Session is not active (already finished).",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "code": 2009,
                                      "message": "Chat session is not active.",
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
    @PostMapping("/{sessionId}/chat")
    ResponseEntity<ApiResponse<ChatMessageResponse>> chat(
            @Parameter(description = "Chat session ID", example = "1") @PathVariable Long sessionId,
            @Valid @RequestBody ChatRequest request,
            @AuthenticationPrincipal PrincipalDetails principal
    );

    @Operation(
            summary = "Finish session and generate recall questions (LLM)",
            description = "Closes the session (status → CLOSED) and calls the LLM to generate recall questions with model answers. " +
                    "Creates an Evaluation record. " +
                    "Returns the question list WITHOUT model answers — answers are only revealed after evaluation submission."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Session closed and recall questions generated.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = FinishSessionResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "code": 1000,
                                      "message": "Request processed successfully.",
                                      "success": true,
                                      "data": {
                                        "evaluationId": 1,
                                        "questions": [
                                          {
                                            "id": 1,
                                            "questionOrder": 1,
                                            "question": "Explain what supervised learning is and provide a real-world example."
                                          },
                                          {
                                            "id": 2,
                                            "questionOrder": 2,
                                            "question": "What is the difference between classification and regression?"
                                          }
                                        ]
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Session not found.",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "code": 4004,
                                      "message": "Chat session not found.",
                                      "success": false,
                                      "data": null
                                    }
                                    """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Session is already closed.",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "code": 2009,
                                      "message": "Chat session is not active.",
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
    @PostMapping("/{sessionId}/finish")
    ResponseEntity<ApiResponse<FinishSessionResponse>> finishSession(
            @Parameter(description = "Chat session ID", example = "1") @PathVariable Long sessionId,
            @AuthenticationPrincipal PrincipalDetails principal
    );

    @Operation(
            summary = "Get evaluation questions for a session",
            description = "Returns the evaluation questions for a finished session. " +
                    "Model answers are hidden until evaluation is submitted. " +
                    "After submission, model answers and scores are included."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Evaluation retrieved successfully. " +
                            "If already submitted, returns full results including model answers.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "code": 1000,
                                      "message": "Request processed successfully.",
                                      "success": true,
                                      "data": {
                                        "evaluationId": 1,
                                        "questions": [
                                          { "id": 1, "questionOrder": 1, "question": "Explain supervised learning." }
                                        ]
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Session or evaluation not found.",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "code": 4004,
                                      "message": "Chat session not found.",
                                      "success": false,
                                      "data": null
                                    }
                                    """))
            )
    })
    @GetMapping("/{sessionId}/evaluation")
    ResponseEntity<ApiResponse<FinishSessionResponse>> getEvaluation(
            @Parameter(description = "Chat session ID", example = "1") @PathVariable Long sessionId,
            @AuthenticationPrincipal PrincipalDetails principal
    );

    @Operation(
            summary = "Submit recall answers and score (Hybrid algorithm + LLM feedback)",
            description = "Accepts all recall answers, runs the hybrid scoring algorithm (Dense 45% + Sparse 30% + Keyword 25%), " +
                    "generates AI feedback via LLM, saves the score, and sets the initial SM-2 review schedule for each question. " +
                    "Model answers are revealed in the response. Returns 409 if answers were already submitted."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Evaluation completed. Returns score, feedback, and per-question results with model answers.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = EvaluationResultResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "code": 1000,
                                      "message": "Request processed successfully.",
                                      "success": true,
                                      "data": {
                                        "evaluationId": 1,
                                        "score": 75,
                                        "feedback": "Good understanding overall. You correctly identified supervised learning but missed some nuances of the bias-variance tradeoff.",
                                        "questions": [
                                          {
                                            "id": 1,
                                            "questionOrder": 1,
                                            "question": "Explain what supervised learning is.",
                                            "modelAnswer": "Supervised learning is a paradigm where a model learns from labeled training data...",
                                            "userAnswer": "Supervised learning uses labeled examples to train a model.",
                                            "nextReviewDate": "2024-01-06"
                                          }
                                        ]
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Session or question not found.",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "code": 4004,
                                      "message": "Chat session not found.",
                                      "success": false,
                                      "data": null
                                    }
                                    """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Evaluation has already been submitted.",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "code": 2011,
                                      "message": "Evaluation has already been submitted.",
                                      "success": false,
                                      "data": null
                                    }
                                    """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "502",
                    description = "LLM API call for feedback generation failed.",
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
    @PostMapping("/{sessionId}/evaluate")
    ResponseEntity<ApiResponse<EvaluationResultResponse>> evaluate(
            @Parameter(description = "Chat session ID", example = "1") @PathVariable Long sessionId,
            @Valid @RequestBody EvaluateRequest request,
            @AuthenticationPrincipal PrincipalDetails principal
    );

    @Operation(
            summary = "Get completed evaluation result",
            description = "Returns the full evaluation result (score, feedback, questions with model answers) for an already-submitted evaluation."
    )
    @GetMapping("/{sessionId}/result")
    ResponseEntity<ApiResponse<EvaluationResultResponse>> getEvaluationResult(
            @Parameter(description = "Chat session ID", example = "1") @PathVariable Long sessionId,
            @AuthenticationPrincipal PrincipalDetails principal
    );
}
