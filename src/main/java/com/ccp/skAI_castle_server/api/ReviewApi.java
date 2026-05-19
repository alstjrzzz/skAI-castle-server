package com.ccp.skAI_castle_server.api;

import com.ccp.skAI_castle_server.dto.request.ReviewCompleteRequest;
import com.ccp.skAI_castle_server.dto.response.ApiResponse;
import com.ccp.skAI_castle_server.dto.response.ReviewCardResponse;
import com.ccp.skAI_castle_server.dto.response.ReviewResultResponse;
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

@Tag(name = "Review", description = "Spaced-repetition review cards and SM-2 scheduling")
@RequestMapping("/reviews")
@SecurityRequirement(name = "BearerAuth")
public interface ReviewApi {

    @Operation(
            summary = "Get today's review cards",
            description = "Returns all evaluation questions whose next_review_date is today or earlier and have not yet been reviewed today. " +
                    "Model answers are intentionally omitted. " +
                    "Returns an empty list if there are no reviews due."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Today's review cards retrieved successfully.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "code": 1000,
                                      "message": "Request processed successfully.",
                                      "success": true,
                                      "data": [
                                        {
                                          "questionId": 1,
                                          "question": "Explain what supervised learning is.",
                                          "topicTitle": "Machine Learning Fundamentals",
                                          "reviewCount": 1,
                                          "reviewDate": "2024-01-05"
                                        },
                                        {
                                          "questionId": 3,
                                          "question": "What is the bias-variance tradeoff?",
                                          "topicTitle": "Machine Learning Fundamentals",
                                          "reviewCount": 0,
                                          "reviewDate": "2024-01-05"
                                        }
                                      ]
                                    }
                                    """)
                    )
            )
    })
    @GetMapping("/today")
    ResponseEntity<ApiResponse<List<ReviewCardResponse>>> getTodayReviews(
            @AuthenticationPrincipal PrincipalDetails principal
    );

    @Operation(
            summary = "Complete a review card (Hybrid scoring + SM-2)",
            description = "Submits the user's answer for a review card. " +
                    "Runs the same hybrid scoring algorithm as recall evaluation (Dense 45% + Sparse 30% + Keyword 25%). " +
                    "Applies the SM-2 algorithm to compute the next review date and updates review_count. " +
                    "Model answer is revealed in the response. No LLM is called during review."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Review completed. Returns score, model answer, and next review date.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ReviewResultResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "code": 1000,
                                      "message": "Request processed successfully.",
                                      "success": true,
                                      "data": {
                                        "score": 85,
                                        "modelAnswer": "Supervised learning is a paradigm where a model learns from labeled training data...",
                                        "nextReviewDate": "2024-01-12",
                                        "reviewCount": 2
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
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Question not found or does not belong to the authenticated user.",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "code": 4006,
                                      "message": "Evaluation question not found.",
                                      "success": false,
                                      "data": null
                                    }
                                    """))
            )
    })
    @PostMapping("/{questionId}/complete")
    ResponseEntity<ApiResponse<ReviewResultResponse>> completeReview(
            @Parameter(description = "Evaluation question ID (review card identifier)", example = "1")
            @PathVariable Long questionId,
            @Valid @RequestBody ReviewCompleteRequest request,
            @AuthenticationPrincipal PrincipalDetails principal
    );
}
