package com.ccp.skAI_castle_server.controller;

import com.ccp.skAI_castle_server.domain.StudyTopicStatus;
import com.ccp.skAI_castle_server.domain.UserRole;
import com.ccp.skAI_castle_server.domain.entity.User;
import com.ccp.skAI_castle_server.dto.response.*;
import com.ccp.skAI_castle_server.exception.ApiException;
import com.ccp.skAI_castle_server.security.JwtProvider;
import com.ccp.skAI_castle_server.service.StudyTopicService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static com.ccp.skAI_castle_server.dto.response.ApiResultCode.TOPIC_NOT_FOUND;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StudyTopicController.class)
class StudyTopicControllerTest extends ControllerTestSupport {

    @Autowired MockMvc mockMvc;
    @MockBean StudyTopicService studyTopicService;
    @MockBean JwtProvider jwtProvider;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .email("test@example.com").password("encoded")
                .nickname("Test").role(UserRole.USER).build();
        given(studyTopicService.loadUser(any())).willReturn(mockUser);
    }

    @Test
    void createTopic_authenticated_returns201() throws Exception {
        StudyTopicResponse response = StudyTopicResponse.builder()
                .id(1L).title("Machine Learning")
                .status(StudyTopicStatus.IN_PROGRESS.name())
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        given(studyTopicService.createTopic("Machine Learning", mockUser)).willReturn(response);

        mockMvc.perform(post("/api/topics")
                        .with(mockAuth())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Machine Learning"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("Machine Learning"))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
    }

    @Test
    void createTopic_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/topics")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Machine Learning"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createTopic_blankTitle_returns400() throws Exception {
        mockMvc.perform(post("/api/topics")
                        .with(mockAuth())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getMyTopics_authenticated_returnsTopicList() throws Exception {
        given(studyTopicService.getMyTopics(mockUser)).willReturn(List.of(
                StudyTopicSummaryResponse.builder()
                        .id(1L).title("ML").status("IN_PROGRESS")
                        .hasOutline(false).createdAt(LocalDateTime.now()).build()
        ));

        mockMvc.perform(get("/api/topics").with(mockAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("ML"))
                .andExpect(jsonPath("$.data[0].hasOutline").value(false));
    }

    @Test
    void generateOutline_authenticated_returns200() throws Exception {
        OutlineDto outline = new OutlineDto(List.of(
                new OutlineDto.ChapterDto("Chapter 1", List.of("concept1", "concept2"))
        ));
        given(studyTopicService.generateOutline(1L, mockUser)).willReturn(outline);

        mockMvc.perform(post("/api/topics/1/outline")
                        .with(mockAuth())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.chapters[0].title").value("Chapter 1"));
    }

    @Test
    void generateOutline_topicNotFound_returns404() throws Exception {
        given(studyTopicService.generateOutline(99L, mockUser))
                .willThrow(new ApiException(TOPIC_NOT_FOUND));

        mockMvc.perform(post("/api/topics/99/outline")
                        .with(mockAuth())
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(4003));
    }

    @Test
    void getTopicHistory_authenticated_returns200() throws Exception {
        TopicHistoryResponse history = TopicHistoryResponse.builder()
                .topicId(1L).title("ML").status("IN_PROGRESS")
                .sessions(List.of()).build();
        given(studyTopicService.getTopicHistory(1L, mockUser)).willReturn(history);

        mockMvc.perform(get("/api/topics/1/history").with(mockAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.topicId").value(1));
    }
}
