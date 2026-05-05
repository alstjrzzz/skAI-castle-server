package com.ccp.skAI_castle_server.service;

import com.ccp.skAI_castle_server.domain.ChatMessageRole;
import com.ccp.skAI_castle_server.domain.ChatSessionStatus;
import com.ccp.skAI_castle_server.domain.entity.*;
import com.ccp.skAI_castle_server.dto.request.EvaluateRequest;
import com.ccp.skAI_castle_server.dto.response.*;
import com.ccp.skAI_castle_server.exception.ApiException;
import com.ccp.skAI_castle_server.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.ccp.skAI_castle_server.dto.response.ApiResultCode.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TutoringService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final EvaluationRepository evaluationRepository;
    private final EvaluationQuestionRepository evaluationQuestionRepository;
    private final StudyTopicRepository studyTopicRepository;
    private final LlmService llmService;
    private final ScoringService scoringService;
    private final Sm2Service sm2Service;
    private final ObjectMapper objectMapper;

    @Transactional
    public ChatSessionResponse createSession(Long topicId, User user) {
        StudyTopic topic = studyTopicRepository.findByIdAndUser(topicId, user)
                .orElseThrow(() -> new ApiException(TOPIC_NOT_FOUND));

        ChatSession session = ChatSession.builder()
                .user(user)
                .studyTopic(topic)
                .build();
        chatSessionRepository.save(session);

        return toSessionResponse(session, List.of());
    }

    @Transactional(readOnly = true)
    public ChatSessionResponse getSession(Long sessionId, User user) {
        ChatSession session = chatSessionRepository.findByIdAndUser(sessionId, user)
                .orElseThrow(() -> new ApiException(SESSION_NOT_FOUND));
        List<ChatMessage> messages = chatMessageRepository.findBySessionOrderByTurnNumberAscIdAsc(session);
        return toSessionResponse(session, messages);
    }

    @Transactional
    public ChatMessageResponse chat(Long sessionId, String userMessage, User user) {
        ChatSession session = chatSessionRepository.findByIdAndUser(sessionId, user)
                .orElseThrow(() -> new ApiException(SESSION_NOT_FOUND));

        if (session.getStatus() != ChatSessionStatus.ACTIVE) {
            throw new ApiException(SESSION_NOT_ACTIVE);
        }

        // Compute turn number
        long userMsgCount = chatMessageRepository.countBySessionAndRole(session, ChatMessageRole.USER);
        int turnNumber = (int) userMsgCount + 1;

        // Save user message
        ChatMessage userMsg = ChatMessage.builder()
                .session(session)
                .role(ChatMessageRole.USER)
                .content(userMessage)
                .turnNumber(turnNumber)
                .build();
        chatMessageRepository.save(userMsg);

        // Load full history (including new user message) and call LLM
        List<ChatMessage> history = chatMessageRepository.findBySessionOrderByTurnNumberAscIdAsc(session);
        String topicTitle = session.getStudyTopic().getTitle();
        String outlineJson = session.getStudyTopic().getOutline();

        String aiResponse = llmService.tutorChat(topicTitle, outlineJson, history);

        // Save AI response
        ChatMessage aiMsg = ChatMessage.builder()
                .session(session)
                .role(ChatMessageRole.ASSISTANT)
                .content(aiResponse)
                .turnNumber(turnNumber)
                .build();
        chatMessageRepository.save(aiMsg);

        return ChatMessageResponse.builder()
                .message(aiResponse)
                .turnNumber(turnNumber)
                .build();
    }

    @Transactional
    public FinishSessionResponse finishSession(Long sessionId, User user) {
        ChatSession session = chatSessionRepository.findByIdAndUser(sessionId, user)
                .orElseThrow(() -> new ApiException(SESSION_NOT_FOUND));

        if (session.getStatus() != ChatSessionStatus.ACTIVE) {
            throw new ApiException(SESSION_NOT_ACTIVE);
        }

        // Close session
        session.close();

        // Load chat history and generate questions
        List<ChatMessage> history = chatMessageRepository.findBySessionOrderByTurnNumberAscIdAsc(session);
        String outlineJson = session.getStudyTopic().getOutline();

        List<LlmService.QuestionData> questionDataList = llmService.generateQuestions(outlineJson, history);

        // Create Evaluation record (score not yet set)
        Evaluation evaluation = Evaluation.builder()
                .user(user)
                .studyTopic(session.getStudyTopic())
                .chatSession(session)
                .build();
        evaluationRepository.save(evaluation);

        // Save generated questions (model answers stored, not exposed to user yet)
        List<FinishSessionResponse.QuestionItem> questionItems = new ArrayList<>();
        for (int i = 0; i < questionDataList.size(); i++) {
            LlmService.QuestionData qd = questionDataList.get(i);
            String keywordsJson = serializeKeywords(qd.keywords());

            EvaluationQuestion question = EvaluationQuestion.builder()
                    .evaluation(evaluation)
                    .questionOrder(i + 1)
                    .question(qd.question())
                    .modelAnswer(qd.modelAnswer())
                    .keywords(keywordsJson)
                    .build();
            evaluationQuestionRepository.save(question);

            questionItems.add(FinishSessionResponse.QuestionItem.builder()
                    .id(question.getId())
                    .questionOrder(i + 1)
                    .question(qd.question())
                    .build());
        }

        return FinishSessionResponse.builder()
                .evaluationId(evaluation.getId())
                .questions(questionItems)
                .build();
    }

    @Transactional(readOnly = true)
    public FinishSessionResponse getEvaluation(Long sessionId, User user) {
        ChatSession session = chatSessionRepository.findByIdAndUser(sessionId, user)
                .orElseThrow(() -> new ApiException(SESSION_NOT_FOUND));

        Evaluation evaluation = evaluationRepository.findByChatSession(session)
                .orElseThrow(() -> new ApiException(EVALUATION_NOT_FOUND));

        List<EvaluationQuestion> questions = evaluationQuestionRepository
                .findByEvaluationOrderByQuestionOrderAsc(evaluation);

        List<FinishSessionResponse.QuestionItem> items = questions.stream()
                .map(q -> FinishSessionResponse.QuestionItem.builder()
                        .id(q.getId())
                        .questionOrder(q.getQuestionOrder())
                        .question(q.getQuestion())
                        .build())
                .collect(Collectors.toList());

        return FinishSessionResponse.builder()
                .evaluationId(evaluation.getId())
                .questions(items)
                .build();
    }

    @Transactional
    public EvaluationResultResponse evaluate(Long sessionId, EvaluateRequest request, User user) {
        ChatSession session = chatSessionRepository.findByIdAndUser(sessionId, user)
                .orElseThrow(() -> new ApiException(SESSION_NOT_FOUND));

        Evaluation evaluation = evaluationRepository.findByChatSession(session)
                .orElseThrow(() -> new ApiException(EVALUATION_NOT_FOUND));

        if (evaluation.getScore() != null) {
            throw new ApiException(EVALUATION_ALREADY_SUBMITTED);
        }

        List<EvaluationQuestion> questions = evaluationQuestionRepository
                .findByEvaluationOrderByQuestionOrderAsc(evaluation);

        Map<Long, EvaluationQuestion> questionMap = questions.stream()
                .collect(Collectors.toMap(EvaluationQuestion::getId, q -> q));

        List<EvaluationResultResponse.QuestionResult> results = new ArrayList<>();
        List<Integer> scores = new ArrayList<>();
        List<String> qTexts = new ArrayList<>();
        List<String> modelAnswers = new ArrayList<>();
        List<String> userAnswers = new ArrayList<>();

        for (EvaluateRequest.UserAnswerItem answerItem : request.getAnswers()) {
            EvaluationQuestion q = questionMap.get(answerItem.getQuestionId());
            if (q == null) throw new ApiException(QUESTION_NOT_FOUND);

            int qScore = scoringService.score(answerItem.getUserAnswer(), q.getModelAnswer(), q.getKeywords());
            scores.add(qScore);

            // Set user answer and initial SM-2 review schedule
            q.submitAnswer(answerItem.getUserAnswer());
            q.initReviewSchedule(LocalDate.now().plusDays(1));

            qTexts.add(q.getQuestion());
            modelAnswers.add(q.getModelAnswer());
            userAnswers.add(answerItem.getUserAnswer());

            results.add(EvaluationResultResponse.QuestionResult.builder()
                    .id(q.getId())
                    .questionOrder(q.getQuestionOrder())
                    .question(q.getQuestion())
                    .modelAnswer(q.getModelAnswer())
                    .userAnswer(answerItem.getUserAnswer())
                    .nextReviewDate(LocalDate.now().plusDays(1))
                    .build());
        }

        int overallScore = (int) scores.stream().mapToInt(Integer::intValue).average().orElse(0);
        String feedback = llmService.generateFeedback(overallScore, qTexts, modelAnswers, userAnswers);
        evaluation.updateResult(overallScore, feedback);

        return EvaluationResultResponse.builder()
                .evaluationId(evaluation.getId())
                .score(overallScore)
                .feedback(feedback)
                .questions(results)
                .build();
    }

    private ChatSessionResponse toSessionResponse(ChatSession session, List<ChatMessage> messages) {
        List<ChatSessionResponse.MessageItem> messageItems = messages.stream()
                .map(m -> ChatSessionResponse.MessageItem.builder()
                        .id(m.getId())
                        .role(m.getRole().name())
                        .content(m.getContent())
                        .turnNumber(m.getTurnNumber())
                        .createdAt(m.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return ChatSessionResponse.builder()
                .id(session.getId())
                .topicId(session.getStudyTopic().getId())
                .topicTitle(session.getStudyTopic().getTitle())
                .status(session.getStatus().name())
                .messages(messageItems)
                .createdAt(session.getCreatedAt())
                .build();
    }

    private String serializeKeywords(List<String> keywords) {
        try {
            return objectMapper.writeValueAsString(keywords);
        } catch (Exception e) {
            return "[]";
        }
    }
}
