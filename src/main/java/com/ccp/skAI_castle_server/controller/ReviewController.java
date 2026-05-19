package com.ccp.skAI_castle_server.controller;

import com.ccp.skAI_castle_server.api.ReviewApi;
import com.ccp.skAI_castle_server.domain.entity.User;
import com.ccp.skAI_castle_server.dto.request.ReviewCompleteRequest;
import com.ccp.skAI_castle_server.dto.response.ApiResponse;
import com.ccp.skAI_castle_server.dto.response.ApiResultCode;
import com.ccp.skAI_castle_server.dto.response.ReviewCardResponse;
import com.ccp.skAI_castle_server.dto.response.ReviewResultResponse;
import com.ccp.skAI_castle_server.security.PrincipalDetails;
import com.ccp.skAI_castle_server.service.ReviewService;
import com.ccp.skAI_castle_server.service.StudyTopicService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ReviewController implements ReviewApi {

    private final ReviewService reviewService;
    private final StudyTopicService studyTopicService;

    @Override
    public ResponseEntity<ApiResponse<List<ReviewCardResponse>>> getTodayReviews(
            @AuthenticationPrincipal PrincipalDetails principal) {
        User user = studyTopicService.loadUser(principal.getUuid());
        return ApiResponse.Rest.success(ApiResultCode.SUCCESS, reviewService.getTodayReviews(user));
    }

    @Override
    public ResponseEntity<ApiResponse<ReviewResultResponse>> completeReview(
            Long questionId,
            ReviewCompleteRequest request,
            @AuthenticationPrincipal PrincipalDetails principal) {
        User user = studyTopicService.loadUser(principal.getUuid());
        return ApiResponse.Rest.success(ApiResultCode.SUCCESS,
                reviewService.completeReview(questionId, request.getAnswer(), user));
    }

    @Override
    public ResponseEntity<ApiResponse<ReviewCardResponse>> getReviewCard(
            Long questionId,
            @AuthenticationPrincipal PrincipalDetails principal) {
        User user = studyTopicService.loadUser(principal.getUuid());
        return ApiResponse.Rest.success(ApiResultCode.SUCCESS,
                reviewService.getReviewCard(questionId, user));
    }

    @Override
    public ResponseEntity<ApiResponse<ReviewResultResponse>> getReviewResult(
            Long questionId,
            @AuthenticationPrincipal PrincipalDetails principal) {
        User user = studyTopicService.loadUser(principal.getUuid());
        return ApiResponse.Rest.success(ApiResultCode.SUCCESS,
                reviewService.getReviewResult(questionId, user));
    }
}
