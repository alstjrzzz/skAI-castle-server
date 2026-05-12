package com.ccp.skAI_castle_server.controller;

import com.ccp.skAI_castle_server.domain.UserRole;
import com.ccp.skAI_castle_server.domain.entity.User;
import com.ccp.skAI_castle_server.dto.response.NotificationResponse;
import com.ccp.skAI_castle_server.security.JwtProvider;
import com.ccp.skAI_castle_server.service.NotificationService;
import com.ccp.skAI_castle_server.service.StudyTopicService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest extends ControllerTestSupport {

    @Autowired MockMvc mockMvc;
    @MockitoBean NotificationService notificationService;
    @MockitoBean StudyTopicService studyTopicService;
    @MockitoBean JwtProvider jwtProvider;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .email("test@example.com").password("encoded")
                .nickname("Test").role(UserRole.USER).build();
        given(studyTopicService.loadUser(any())).willReturn(mockUser);
    }

    @Test
    void getNotifications_authenticated_returnsNotificationList() throws Exception {
        given(notificationService.getNotifications(mockUser)).willReturn(List.of(
                NotificationResponse.builder()
                        .id(1L)
                        .message("오늘 복습할 카드가 3개 있습니다.")
                        .link("/reviews/today")
                        .isRead(false)
                        .createdAt(LocalDateTime.now())
                        .build()
        ));

        mockMvc.perform(get("/api/notifications").with(mockAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].link").value("/reviews/today"));
    }

    @Test
    void getNotifications_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void markAsRead_authenticated_returns200() throws Exception {
        willDoNothing().given(notificationService).markAsRead(1L, mockUser);

        mockMvc.perform(patch("/api/notifications/1/read")
                        .with(mockAuth())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        then(notificationService).should().markAsRead(1L, mockUser);
    }

    @Test
    void markAllAsRead_authenticated_returns200() throws Exception {
        willDoNothing().given(notificationService).markAllAsRead(mockUser);

        mockMvc.perform(patch("/api/notifications/read-all")
                        .with(mockAuth())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        then(notificationService).should().markAllAsRead(mockUser);
    }

    @Test
    void markAllAsRead_unauthenticated_returns401() throws Exception {
        mockMvc.perform(patch("/api/notifications/read-all").with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}

