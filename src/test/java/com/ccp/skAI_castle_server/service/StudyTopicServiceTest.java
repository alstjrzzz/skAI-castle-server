package com.ccp.skAI_castle_server.service;

import com.ccp.skAI_castle_server.domain.StudyTopicStatus;
import com.ccp.skAI_castle_server.domain.UserRole;
import com.ccp.skAI_castle_server.domain.entity.StudyTopic;
import com.ccp.skAI_castle_server.domain.entity.User;
import com.ccp.skAI_castle_server.dto.response.OutlineDto;
import com.ccp.skAI_castle_server.dto.response.StudyTopicResponse;
import com.ccp.skAI_castle_server.dto.response.StudyTopicSummaryResponse;
import com.ccp.skAI_castle_server.dto.response.TopicHistoryResponse;
import com.ccp.skAI_castle_server.exception.ApiException;
import com.ccp.skAI_castle_server.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static com.ccp.skAI_castle_server.dto.response.ApiResultCode.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class StudyTopicServiceTest {

    @Mock StudyTopicRepository studyTopicRepository;
    @Mock ChatSessionRepository chatSessionRepository;
    @Mock ChatMessageRepository chatMessageRepository;
    @Mock EvaluationRepository evaluationRepository;
    @Mock EvaluationQuestionRepository evaluationQuestionRepository;
    @Mock UserRepository userRepository;
    @Mock LlmService llmService;

    private StudyTopicService studyTopicService;
    private User testUser;

    @BeforeEach
    void setUp() {
        studyTopicService = new StudyTopicService(
                studyTopicRepository, chatSessionRepository, chatMessageRepository,
                evaluationRepository, evaluationQuestionRepository, userRepository,
                llmService, new ObjectMapper()
        );
        testUser = User.builder()
                .email("test@example.com").password("encoded")
                .nickname("TestUser").role(UserRole.USER).build();
    }

    @Test
    void createTopic_success_returnsMappedResponse() {
        StudyTopic savedTopic = StudyTopic.builder()
                .user(testUser).title("Machine Learning").build();
        ReflectionTestUtils.setField(savedTopic, "id", 1L);
        given(studyTopicRepository.save(any(StudyTopic.class))).willReturn(savedTopic);

        StudyTopicResponse response = studyTopicService.createTopic("Machine Learning", testUser);

        assertThat(response.getTitle()).isEqualTo("Machine Learning");
        assertThat(response.getStatus()).isEqualTo(StudyTopicStatus.IN_PROGRESS.name());
    }

    @Test
    void getMyTopics_returnsMappedSummaryList() {
        StudyTopic topic = StudyTopic.builder()
                .user(testUser).title("Deep Learning").build();
        ReflectionTestUtils.setField(topic, "id", 1L);
        given(studyTopicRepository.findByUserOrderByCreatedAtDesc(testUser)).willReturn(List.of(topic));

        List<StudyTopicSummaryResponse> result = studyTopicService.getMyTopics(testUser);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Deep Learning");
        assertThat(result.get(0).isHasOutline()).isFalse();
    }

    @Test
    void getTopic_notFound_throwsApiException() {
        given(studyTopicRepository.findByIdAndUser(99L, testUser)).willReturn(Optional.empty());

        assertThatThrownBy(() -> studyTopicService.getTopic(99L, testUser))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getResultCode()).isEqualTo(TOPIC_NOT_FOUND));
    }

    @Test
    void generateOutline_success_updatesTopicAndReturnsOutline() {
        StudyTopic topic = StudyTopic.builder()
                .user(testUser).title("Python").build();
        ReflectionTestUtils.setField(topic, "id", 1L);

        OutlineDto outline = new OutlineDto(List.of(
                new OutlineDto.ChapterDto("Basics", List.of("variables", "loops"))
        ));

        given(studyTopicRepository.findByIdAndUser(1L, testUser)).willReturn(Optional.of(topic));
        given(llmService.generateOutline("Python")).willReturn(outline);

        OutlineDto result = studyTopicService.generateOutline(1L, testUser);

        assertThat(result.getChapters()).hasSize(1);
        assertThat(result.getChapters().get(0).getTitle()).isEqualTo("Basics");
        // Verify the entity was mutated (dirty checking handles persistence)
        assertThat(topic.getOutline()).isNotNull();
    }

    @Test
    void generateOutline_alreadyExists_throwsApiException() {
        StudyTopic topicWithOutline = StudyTopic.builder()
                .user(testUser).title("Python").outline("{\"chapters\":[]}").build();
        ReflectionTestUtils.setField(topicWithOutline, "id", 1L);

        given(studyTopicRepository.findByIdAndUser(1L, testUser)).willReturn(Optional.of(topicWithOutline));

        assertThatThrownBy(() -> studyTopicService.generateOutline(1L, testUser))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getResultCode()).isEqualTo(OUTLINE_ALREADY_EXISTS));
    }

    @Test
    void getTopicHistory_returnsSessions() {
        StudyTopic topic = StudyTopic.builder()
                .user(testUser).title("NLP").build();
        ReflectionTestUtils.setField(topic, "id", 1L);

        given(studyTopicRepository.findByIdAndUser(1L, testUser)).willReturn(Optional.of(topic));
        given(chatSessionRepository.findByStudyTopicOrderByCreatedAtAsc(topic)).willReturn(List.of());

        TopicHistoryResponse result = studyTopicService.getTopicHistory(1L, testUser);

        assertThat(result.getTopicId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("NLP");
        assertThat(result.getSessions()).isEmpty();
    }
}
