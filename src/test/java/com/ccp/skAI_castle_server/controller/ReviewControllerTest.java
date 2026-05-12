package com.ccp.skAI_castle_server.controller;

import com.ccp.skAI_castle_server.domain.UserRole;
import com.ccp.skAI_castle_server.domain.entity.User;
import com.ccp.skAI_castle_server.dto.response.ReviewCardResponse;
import com.ccp.skAI_castle_server.dto.response.ReviewResultResponse;
import com.ccp.skAI_castle_server.security.JwtProvider;
import com.ccp.skAI_castle_server.service.ReviewService;
import com.ccp.skAI_castle_server.service.StudyTopicService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReviewController.class)
class ReviewControllerTest extends ControllerTestSupport {

    @Autowired MockMvc mockMvc;
    @MockitoBean ReviewService reviewService;
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
    void getTodayReviews_authenticated_returns200WithCards() throws Exception {
        given(reviewService.getTodayReviews(mockUser)).willReturn(List.of(
                ReviewCardResponse.builder()
                        .questionId(1L)
                        .question("What is gradient descent?")
                        .topicTitle("ML Fundamentals")
                        .reviewCount(1)
                        .reviewDate(LocalDate.now())
                        .build()
        ));

        mockMvc.perform(get("/api/reviews/today").with(mockAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].questionId").value(1))
                .andExpect(jsonPath("$.data[0].question").value("What is gradient descent?"))
                .andExpect(jsonPath("$.data[0].topicTitle").value("ML Fundamentals"));
    }

    @Test
    void getTodayReviews_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/reviews/today"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getTodayReviews_noCards_returnsEmptyList() throws Exception {
        given(reviewService.getTodayReviews(mockUser)).willReturn(List.of());

        mockMvc.perform(get("/api/reviews/today").with(mockAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void completeReview_validAnswer_returns200WithResult() throws Exception {
        ReviewResultResponse result = ReviewResultResponse.builder()
                .score(75)
                .modelAnswer("Gradient descent is an optimization algorithm...")
                .nextReviewDate(LocalDate.now().plusDays(6))
                .reviewCount(2)
                .build();
        given(reviewService.completeReview(eq(1L), eq("An optimization algorithm"), eq(mockUser)))
                .willReturn(result);

        mockMvc.perform(post("/api/reviews/1/complete")
                        .with(mockAuth())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"answer":"An optimization algorithm"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.score").value(75))
                .andExpect(jsonPath("$.data.reviewCount").value(2));
    }

    @Test
    void completeReview_blankAnswer_returns400() throws Exception {
        mockMvc.perform(post("/api/reviews/1/complete")
                        .with(mockAuth())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"answer":""}
                                """))
                .andExpect(status().isBadRequest());
    }
}

