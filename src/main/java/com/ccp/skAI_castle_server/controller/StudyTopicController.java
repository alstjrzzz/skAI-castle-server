package com.ccp.skAI_castle_server.controller;

import com.ccp.skAI_castle_server.api.StudyTopicApi;
import com.ccp.skAI_castle_server.domain.entity.User;
import com.ccp.skAI_castle_server.dto.request.StudyTopicCreateRequest;
import com.ccp.skAI_castle_server.dto.response.*;
import com.ccp.skAI_castle_server.security.PrincipalDetails;
import com.ccp.skAI_castle_server.service.StudyTopicService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class StudyTopicController implements StudyTopicApi {

    private final StudyTopicService studyTopicService;

    @Override
    public ResponseEntity<ApiResponse<StudyTopicResponse>> createTopic(
            StudyTopicCreateRequest request,
            @AuthenticationPrincipal PrincipalDetails principal) {
        User user = studyTopicService.loadUser(principal.getUuid());
        StudyTopicResponse response = studyTopicService.createTopic(request.getTitle(), user);
        return ApiResponse.Rest.success(ApiResultCode.CREATED, response);
    }

    @Override
    public ResponseEntity<ApiResponse<List<StudyTopicSummaryResponse>>> getMyTopics(
            @AuthenticationPrincipal PrincipalDetails principal) {
        User user = studyTopicService.loadUser(principal.getUuid());
        return ApiResponse.Rest.success(ApiResultCode.SUCCESS, studyTopicService.getMyTopics(user));
    }

    @Override
    public ResponseEntity<ApiResponse<StudyTopicResponse>> getTopic(
            Long topicId,
            @AuthenticationPrincipal PrincipalDetails principal) {
        User user = studyTopicService.loadUser(principal.getUuid());
        return ApiResponse.Rest.success(ApiResultCode.SUCCESS, studyTopicService.getTopic(topicId, user));
    }

    @Override
    public ResponseEntity<ApiResponse<OutlineDto>> generateOutline(
            Long topicId,
            @AuthenticationPrincipal PrincipalDetails principal) {
        User user = studyTopicService.loadUser(principal.getUuid());
        return ApiResponse.Rest.success(ApiResultCode.SUCCESS, studyTopicService.generateOutline(topicId, user));
    }

    @Override
    public ResponseEntity<ApiResponse<TopicHistoryResponse>> getTopicHistory(
            Long topicId,
            @AuthenticationPrincipal PrincipalDetails principal) {
        User user = studyTopicService.loadUser(principal.getUuid());
        return ApiResponse.Rest.success(ApiResultCode.SUCCESS, studyTopicService.getTopicHistory(topicId, user));
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> deleteTopic(
            Long topicId,
            @AuthenticationPrincipal PrincipalDetails principal) {
        User user = studyTopicService.loadUser(principal.getUuid());
        studyTopicService.deleteTopic(topicId, user);
        return ApiResponse.Rest.success(ApiResultCode.SUCCESS, null);
    }
}
