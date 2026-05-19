package com.ccp.skAI_castle_server.controller;

import com.ccp.skAI_castle_server.domain.UserRole;
import com.ccp.skAI_castle_server.domain.entity.User;
import com.ccp.skAI_castle_server.dto.response.*;
import com.ccp.skAI_castle_server.exception.ApiException;
import com.ccp.skAI_castle_server.security.JwtProvider;
import com.ccp.skAI_castle_server.service.StudyTopicService;
import com.ccp.skAI_castle_server.service.TutoringService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static com.ccp.skAI_castle_server.dto.response.ApiResultCode.SESSION_NOT_ACTIVE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TutoringController.class)
class TutoringControllerTest extends ControllerTestSupport {

    @Autowired MockMvc mockMvc;
    @MockitoBean TutoringService tutoringService;
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
    void createSession_authenticated_returns201() throws Exception {
        ChatSessionResponse session = ChatSessionResponse.builder()
                .id(10L).topicId(1L).topicTitle("ML").status("ACTIVE")
                .messages(List.of()).createdAt(LocalDateTime.now()).build();
        given(tutoringService.createSession(1L, mockUser)).willReturn(session);

        mockMvc.perform(post("/sessions")
                        .with(mockAuth())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"topicId":1}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void createSession_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/sessions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"topicId":1}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getSession_authenticated_returns200() throws Exception {
        ChatSessionResponse session = ChatSessionResponse.builder()
                .id(10L).topicId(1L).topicTitle("ML").status("ACTIVE")
                .messages(List.of()).createdAt(LocalDateTime.now()).build();
        given(tutoringService.getSession(10L, mockUser)).willReturn(session);

        mockMvc.perform(get("/sessions/10").with(mockAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(10));
    }

    @Test
    void chat_activeSession_returns200WithAiMessage() throws Exception {
        ChatMessageResponse aiMsg = ChatMessageResponse.builder()
                .message("That's a great question! Overfitting occurs when...")
                .turnNumber(1).build();
        given(tutoringService.chat(eq(10L), eq("What is overfitting?"), eq(mockUser))).willReturn(aiMsg);

        mockMvc.perform(post("/sessions/10/chat")
                        .with(mockAuth())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"What is overfitting?"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("That's a great question! Overfitting occurs when..."))
                .andExpect(jsonPath("$.data.turnNumber").value(1));
    }

    @Test
    void chat_closedSession_returns409() throws Exception {
        given(tutoringService.chat(eq(10L), any(), eq(mockUser)))
                .willThrow(new ApiException(SESSION_NOT_ACTIVE));

        mockMvc.perform(post("/sessions/10/chat")
                        .with(mockAuth())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"hello"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(2009));
    }

    @Test
    void finishSession_authenticated_returns200WithQuestions() throws Exception {
        FinishSessionResponse finish = FinishSessionResponse.builder()
                .evaluationId(5L)
                .questions(List.of(
                        FinishSessionResponse.QuestionItem.builder()
                                .id(1L).questionOrder(1).question("Q1?").build()
                ))
                .build();
        given(tutoringService.finishSession(10L, mockUser)).willReturn(finish);

        mockMvc.perform(post("/sessions/10/finish")
                        .with(mockAuth())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.evaluationId").value(5))
                .andExpect(jsonPath("$.data.questions[0].question").value("Q1?"));
    }

    @Test
    void evaluate_validAnswers_returns200WithScoreAndFeedback() throws Exception {
        EvaluationResultResponse result = EvaluationResultResponse.builder()
                .evaluationId(5L).score(80)
                .feedback("Good understanding!")
                .questions(List.of())
                .build();
        given(tutoringService.evaluate(eq(10L), any(), eq(mockUser))).willReturn(result);

        mockMvc.perform(post("/sessions/10/evaluate")
                        .with(mockAuth())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"answers":[{"questionId":1,"userAnswer":"Overfitting is when the model memorizes training data."}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.score").value(80))
                .andExpect(jsonPath("$.data.feedback").value("Good understanding!"));
    }

    @Test
    void evaluate_emptyAnswers_returns400() throws Exception {
        mockMvc.perform(post("/sessions/10/evaluate")
                        .with(mockAuth())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"answers":[]}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getEvaluationResult_authenticated_returns200() throws Exception {
        EvaluationResultResponse result = EvaluationResultResponse.builder()
                .evaluationId(5L).score(75)
                .feedback("잘했어요!")
                .questions(List.of())
                .build();
        given(tutoringService.getEvaluationResult(10L, mockUser)).willReturn(result);

        mockMvc.perform(get("/sessions/10/result").with(mockAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.score").value(75))
                .andExpect(jsonPath("$.data.feedback").value("잘했어요!"));
    }

    @Test
    void getEvaluationResult_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/sessions/10/result"))
                .andExpect(status().isUnauthorized());
    }
}

