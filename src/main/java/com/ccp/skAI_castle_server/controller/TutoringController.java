package com.ccp.skAI_castle_server.controller;

import com.ccp.skAI_castle_server.api.TutoringApi;
import com.ccp.skAI_castle_server.domain.entity.User;
import com.ccp.skAI_castle_server.dto.request.ChatRequest;
import com.ccp.skAI_castle_server.dto.request.ChatSessionCreateRequest;
import com.ccp.skAI_castle_server.dto.request.EvaluateRequest;
import com.ccp.skAI_castle_server.dto.response.*;
import com.ccp.skAI_castle_server.security.PrincipalDetails;
import com.ccp.skAI_castle_server.service.StudyTopicService;
import com.ccp.skAI_castle_server.service.TutoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TutoringController implements TutoringApi {

    private final TutoringService tutoringService;
    private final StudyTopicService studyTopicService;

    @Override
    public ResponseEntity<ApiResponse<ChatSessionResponse>> createSession(
            ChatSessionCreateRequest request,
            @AuthenticationPrincipal PrincipalDetails principal) {
        User user = studyTopicService.loadUser(principal.getUuid());
        return ApiResponse.Rest.success(ApiResultCode.CREATED, tutoringService.createSession(request.getTopicId(), user));
    }

    @Override
    public ResponseEntity<ApiResponse<ChatSessionResponse>> getSession(
            Long sessionId,
            @AuthenticationPrincipal PrincipalDetails principal) {
        User user = studyTopicService.loadUser(principal.getUuid());
        return ApiResponse.Rest.success(ApiResultCode.SUCCESS, tutoringService.getSession(sessionId, user));
    }

    @Override
    public ResponseEntity<ApiResponse<ChatMessageResponse>> chat(
            Long sessionId,
            ChatRequest request,
            @AuthenticationPrincipal PrincipalDetails principal) {
        User user = studyTopicService.loadUser(principal.getUuid());
        return ApiResponse.Rest.success(ApiResultCode.SUCCESS, tutoringService.chat(sessionId, request.getMessage(), user));
    }

    @Override
    public ResponseEntity<ApiResponse<FinishSessionResponse>> finishSession(
            Long sessionId,
            @AuthenticationPrincipal PrincipalDetails principal) {
        User user = studyTopicService.loadUser(principal.getUuid());
        return ApiResponse.Rest.success(ApiResultCode.SUCCESS, tutoringService.finishSession(sessionId, user));
    }

    @Override
    public ResponseEntity<ApiResponse<FinishSessionResponse>> getEvaluation(
            Long sessionId,
            @AuthenticationPrincipal PrincipalDetails principal) {
        User user = studyTopicService.loadUser(principal.getUuid());
        return ApiResponse.Rest.success(ApiResultCode.SUCCESS, tutoringService.getEvaluation(sessionId, user));
    }

    @Override
    public ResponseEntity<ApiResponse<EvaluationResultResponse>> evaluate(
            Long sessionId,
            EvaluateRequest request,
            @AuthenticationPrincipal PrincipalDetails principal) {
        User user = studyTopicService.loadUser(principal.getUuid());
        return ApiResponse.Rest.success(ApiResultCode.SUCCESS, tutoringService.evaluate(sessionId, request, user));
    }
}
