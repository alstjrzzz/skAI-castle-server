package com.ccp.skAI_castle_server.service;

import com.ccp.skAI_castle_server.domain.ChatMessageRole;
import com.ccp.skAI_castle_server.domain.ChatSessionStatus;
import com.ccp.skAI_castle_server.domain.UserRole;
import com.ccp.skAI_castle_server.domain.entity.*;
import com.ccp.skAI_castle_server.dto.request.EvaluateRequest;
import com.ccp.skAI_castle_server.dto.response.ChatMessageResponse;
import com.ccp.skAI_castle_server.dto.response.FinishSessionResponse;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class TutoringServiceTest {

    @Mock ChatSessionRepository chatSessionRepository;
    @Mock ChatMessageRepository chatMessageRepository;
    @Mock EvaluationRepository evaluationRepository;
    @Mock EvaluationQuestionRepository evaluationQuestionRepository;
    @Mock StudyTopicRepository studyTopicRepository;
    @Mock LlmService llmService;
    @Mock ScoringService scoringService;
    @Mock Sm2Service sm2Service;

    private TutoringService tutoringService;
    private User testUser;
    private StudyTopic testTopic;

    @BeforeEach
    void setUp() {
        tutoringService = new TutoringService(
                chatSessionRepository, chatMessageRepository, evaluationRepository,
                evaluationQuestionRepository, studyTopicRepository,
                llmService, scoringService, sm2Service, new ObjectMapper()
        );
        testUser = User.builder()
                .email("test@example.com").password("encoded")
                .nickname("Test").role(UserRole.USER).build();
        testTopic = StudyTopic.builder()
                .user(testUser).title("ML Fundamentals").build();
        ReflectionTestUtils.setField(testTopic, "id", 1L);
    }

    @Test
    void createSession_success_returnsActiveSession() {
        given(studyTopicRepository.findByIdAndUser(1L, testUser)).willReturn(Optional.of(testTopic));
        ChatSession session = ChatSession.builder().user(testUser).studyTopic(testTopic).build();
        ReflectionTestUtils.setField(session, "id", 10L);
        given(chatSessionRepository.save(any(ChatSession.class))).willReturn(session);

        var response = tutoringService.createSession(1L, testUser);

        assertThat(response.getTopicId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo(ChatSessionStatus.ACTIVE.name());
        assertThat(response.getMessages()).isEmpty();
    }

    @Test
    void createSession_topicNotFound_throwsApiException() {
        given(studyTopicRepository.findByIdAndUser(99L, testUser)).willReturn(Optional.empty());

        assertThatThrownBy(() -> tutoringService.createSession(99L, testUser))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getResultCode()).isEqualTo(TOPIC_NOT_FOUND));
    }

    @Test
    void chat_activeSession_savesMessagesAndReturnsAiResponse() {
        ChatSession session = ChatSession.builder().user(testUser).studyTopic(testTopic).build();
        ReflectionTestUtils.setField(session, "id", 10L);

        given(chatSessionRepository.findByIdAndUser(10L, testUser)).willReturn(Optional.of(session));
        given(chatMessageRepository.countBySessionAndRole(session, ChatMessageRole.USER)).willReturn(0L);
        given(chatMessageRepository.findBySessionOrderByTurnNumberAscIdAsc(session)).willReturn(List.of());
        given(llmService.tutorChat(anyString(), any(), anyList())).willReturn("Great question! ...");

        ChatMessageResponse response = tutoringService.chat(10L, "What is overfitting?", testUser);

        assertThat(response.getMessage()).isEqualTo("Great question! ...");
        assertThat(response.getTurnNumber()).isEqualTo(1);
    }

    @Test
    void chat_closedSession_throwsApiException() {
        ChatSession closed = mock(ChatSession.class);
        given(closed.getStatus()).willReturn(ChatSessionStatus.CLOSED);
        given(chatSessionRepository.findByIdAndUser(10L, testUser)).willReturn(Optional.of(closed));

        assertThatThrownBy(() -> tutoringService.chat(10L, "hello", testUser))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getResultCode()).isEqualTo(SESSION_NOT_ACTIVE));
    }

    @Test
    void finishSession_success_createsEvaluationAndQuestions() {
        ChatSession session = ChatSession.builder().user(testUser).studyTopic(testTopic).build();
        ReflectionTestUtils.setField(session, "id", 10L);

        given(chatSessionRepository.findByIdAndUser(10L, testUser)).willReturn(Optional.of(session));
        given(chatMessageRepository.findBySessionOrderByTurnNumberAscIdAsc(session)).willReturn(List.of());
        given(llmService.generateQuestions(any(), anyList())).willReturn(List.of(
                new LlmService.QuestionData("Q1?", "Model A1", List.of("key1")),
                new LlmService.QuestionData("Q2?", "Model A2", List.of("key2"))
        ));
        Evaluation savedEval = Evaluation.builder()
                .user(testUser).studyTopic(testTopic).chatSession(session).build();
        ReflectionTestUtils.setField(savedEval, "id", 5L);
        given(evaluationRepository.save(any(Evaluation.class))).willReturn(savedEval);

        EvaluationQuestion q1 = EvaluationQuestion.builder()
                .evaluation(savedEval).questionOrder(1).question("Q1?")
                .modelAnswer("A1").keywords("[\"key1\"]").build();
        EvaluationQuestion q2 = EvaluationQuestion.builder()
                .evaluation(savedEval).questionOrder(2).question("Q2?")
                .modelAnswer("A2").keywords("[\"key2\"]").build();
        ReflectionTestUtils.setField(q1, "id", 1L);
        ReflectionTestUtils.setField(q2, "id", 2L);
        given(evaluationQuestionRepository.save(any(EvaluationQuestion.class))).willReturn(q1, q2);

        FinishSessionResponse response = tutoringService.finishSession(10L, testUser);

        assertThat(response.getEvaluationId()).isEqualTo(5L);
        assertThat(response.getQuestions()).hasSize(2);
        assertThat(response.getQuestions().get(0).getQuestion()).isEqualTo("Q1?");
    }

    @Test
    void finishSession_alreadyClosed_throwsApiException() {
        ChatSession closed = mock(ChatSession.class);
        given(closed.getStatus()).willReturn(ChatSessionStatus.CLOSED);
        given(chatSessionRepository.findByIdAndUser(10L, testUser)).willReturn(Optional.of(closed));

        assertThatThrownBy(() -> tutoringService.finishSession(10L, testUser))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getResultCode()).isEqualTo(SESSION_NOT_ACTIVE));
    }

    @Test
    void evaluate_alreadySubmitted_throwsApiException() {
        ChatSession session = ChatSession.builder().user(testUser).studyTopic(testTopic).build();
        ReflectionTestUtils.setField(session, "id", 10L);

        Evaluation alreadyScored = Evaluation.builder()
                .user(testUser).studyTopic(testTopic).chatSession(session).build();
        alreadyScored.updateResult(80, "Good job");

        given(chatSessionRepository.findByIdAndUser(10L, testUser)).willReturn(Optional.of(session));
        given(evaluationRepository.findByChatSession(session)).willReturn(Optional.of(alreadyScored));

        EvaluateRequest request = mock(EvaluateRequest.class);
        assertThatThrownBy(() -> tutoringService.evaluate(10L, request, testUser))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getResultCode()).isEqualTo(EVALUATION_ALREADY_SUBMITTED));
    }
}
