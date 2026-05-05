package com.ccp.skAI_castle_server.controller;

import com.ccp.skAI_castle_server.api.NotificationApi;
import com.ccp.skAI_castle_server.domain.entity.User;
import com.ccp.skAI_castle_server.dto.response.ApiResponse;
import com.ccp.skAI_castle_server.dto.response.ApiResultCode;
import com.ccp.skAI_castle_server.dto.response.NotificationResponse;
import com.ccp.skAI_castle_server.security.PrincipalDetails;
import com.ccp.skAI_castle_server.service.NotificationService;
import com.ccp.skAI_castle_server.service.StudyTopicService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class NotificationController implements NotificationApi {

    private final NotificationService notificationService;
    private final StudyTopicService studyTopicService;

    @Override
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getNotifications(
            @AuthenticationPrincipal PrincipalDetails principal) {
        User user = studyTopicService.loadUser(principal.getUuid());
        return ApiResponse.Rest.success(ApiResultCode.SUCCESS, notificationService.getNotifications(user));
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            Long notificationId,
            @AuthenticationPrincipal PrincipalDetails principal) {
        User user = studyTopicService.loadUser(principal.getUuid());
        notificationService.markAsRead(notificationId, user);
        return ApiResponse.Rest.success(ApiResultCode.SUCCESS);
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal PrincipalDetails principal) {
        User user = studyTopicService.loadUser(principal.getUuid());
        notificationService.markAllAsRead(user);
        return ApiResponse.Rest.success(ApiResultCode.SUCCESS);
    }
}
