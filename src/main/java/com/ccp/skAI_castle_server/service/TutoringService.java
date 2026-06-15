package com.ccp.skAI_castle_server.service;

import com.ccp.skAI_castle_server.domain.ChatMessageRole;
import com.ccp.skAI_castle_server.domain.ChatSessionStatus;
import com.ccp.skAI_castle_server.domain.QuestionType;
import com.ccp.skAI_castle_server.domain.entity.*;
import com.ccp.skAI_castle_server.dto.request.EvaluateRequest;
import com.ccp.skAI_castle_server.dto.response.*;
import com.ccp.skAI_castle_server.exception.ApiException;
import com.ccp.skAI_castle_server.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

        long userMsgCount = chatMessageRepository.countBySessionAndRole(session, ChatMessageRole.USER);
        int turnNumber = (int) userMsgCount + 1;

        ChatMessage userMsg = ChatMessage.builder()
                .session(session)
                .role(ChatMessageRole.USER)
                .content(userMessage)
                .turnNumber(turnNumber)
                .build();
        chatMessageRepository.save(userMsg);

        List<ChatMessage> history = chatMessageRepository.findBySessionOrderByTurnNumberAscIdAsc(session);
        String topicTitle = session.getStudyTopic().getTitle();
        String outlineJson = session.getStudyTopic().getOutline();

        String aiResponse = llmService.tutorChat(topicTitle, outlineJson, history);

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

        session.close();

        List<ChatMessage> history = chatMessageRepository.findBySessionOrderByTurnNumberAscIdAsc(session);
        String outlineJson = session.getStudyTopic().getOutline();

        // Generate pool of 10 questions (mix of WRITTEN + MULTIPLE_CHOICE)
        List<LlmService.QuestionData> questionDataList = llmService.generateQuestions(outlineJson, history);

        // Randomly select 4 questions as recall (shown immediately); rest enter review queue directly
        List<Integer> indices = IntStream.range(0, questionDataList.size()).boxed().collect(Collectors.toList());
        Collections.shuffle(indices);
        int recallCount = Math.min(4, questionDataList.size());
        Set<Integer> recallIndices = new HashSet<>(indices.subList(0, recallCount));

        Evaluation evaluation = evaluationRepository.save(Evaluation.builder()
                .user(user)
                .studyTopic(session.getStudyTopic())
                .chatSession(session)
                .build());

        List<FinishSessionResponse.QuestionItem> recallItems = new ArrayList<>();
        int recallOrder = 0;

        for (int i = 0; i < questionDataList.size(); i++) {
            LlmService.QuestionData qd = questionDataList.get(i);
            boolean isRecall = recallIndices.contains(i);
            String keywordsJson = serializeKeywords(qd.keywords());

            EvaluationQuestion saved = evaluationQuestionRepository.save(EvaluationQuestion.builder()
                    .evaluation(evaluation)
                    .questionOrder(i + 1)
                    .question(qd.question())
                    .modelAnswer(qd.modelAnswer())
                    .keywords(keywordsJson)
                    .questionType(qd.questionType())
                    .choices(qd.choicesJson())
                    .isRecallQuestion(isRecall)
                    .build());

            if (!isRecall) {
                // Non-recall questions skip the evaluation step and go straight to the review queue
                saved.initReviewSchedule(LocalDate.now().plusDays(1));
            }

            if (isRecall) {
                recallOrder++;
                recallItems.add(FinishSessionResponse.QuestionItem.builder()
                        .id(saved.getId())
                        .questionOrder(recallOrder)
                        .question(qd.question())
                        .questionType(qd.questionType())
                        .choices(parseChoices(qd.choicesJson(), false))
                        .build());
            }
        }

        return FinishSessionResponse.builder()
                .evaluationId(evaluation.getId())
                .questions(recallItems)
                .build();
    }

    @Transactional(readOnly = true)
    public FinishSessionResponse getEvaluation(Long sessionId, User user) {
        ChatSession session = chatSessionRepository.findByIdAndUser(sessionId, user)
                .orElseThrow(() -> new ApiException(SESSION_NOT_FOUND));

        Evaluation evaluation = evaluationRepository.findByChatSession(session)
                .orElseThrow(() -> new ApiException(EVALUATION_NOT_FOUND));

        List<EvaluationQuestion> recallQuestions = evaluationQuestionRepository
                .findByEvaluationOrderByQuestionOrderAsc(evaluation)
                .stream()
                .filter(EvaluationQuestion::isRecall)
                .collect(Collectors.toList());

        List<FinishSessionResponse.QuestionItem> items = new ArrayList<>();
        for (int i = 0; i < recallQuestions.size(); i++) {
            EvaluationQuestion q = recallQuestions.get(i);
            items.add(FinishSessionResponse.QuestionItem.builder()
                    .id(q.getId())
                    .questionOrder(i + 1)
                    .question(q.getQuestion())
                    .questionType(q.getQuestionType())
                    .choices(parseChoices(q.getChoices(), false))
                    .build());
        }

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

        int displayOrder = 0;
        for (EvaluateRequest.UserAnswerItem answerItem : request.getAnswers()) {
            EvaluationQuestion q = questionMap.get(answerItem.getQuestionId());
            if (q == null) throw new ApiException(QUESTION_NOT_FOUND);

            int qScore;
            String answeredText;

            if (q.getQuestionType() == QuestionType.MULTIPLE_CHOICE) {
                String correctLabel = findCorrectLabel(q.getChoices());
                qScore = correctLabel.equals(answerItem.getSelectedChoice()) ? 100 : 0;
                answeredText = answerItem.getSelectedChoice();
            } else {
                qScore = scoringService.score(q.getQuestion(), answerItem.getUserAnswer(), q.getModelAnswer(), q.getKeywords());
                answeredText = answerItem.getUserAnswer();
            }

            scores.add(qScore);
            q.submitAnswer(answeredText, qScore);
            q.initReviewSchedule(LocalDate.now().plusDays(1));

            qTexts.add(q.getQuestion());
            modelAnswers.add(q.getModelAnswer());
            userAnswers.add(answeredText != null ? answeredText : "");

            displayOrder++;
            results.add(EvaluationResultResponse.QuestionResult.builder()
                    .id(q.getId())
                    .questionOrder(displayOrder)
                    .question(q.getQuestion())
                    .questionType(q.getQuestionType())
                    .modelAnswer(q.getModelAnswer())
                    .userAnswer(answeredText)
                    .score(qScore)
                    .nextReviewDate(LocalDate.now().plusDays(1))
                    .choices(parseChoices(q.getChoices(), true))
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

    @Transactional(readOnly = true)
    public EvaluationResultResponse getEvaluationResult(Long sessionId, User user) {
        ChatSession session = chatSessionRepository.findByIdAndUser(sessionId, user)
                .orElseThrow(() -> new ApiException(SESSION_NOT_FOUND));

        Evaluation evaluation = evaluationRepository.findByChatSession(session)
                .orElseThrow(() -> new ApiException(EVALUATION_NOT_FOUND));

        if (evaluation.getScore() == null) {
            throw new ApiException(EVALUATION_NOT_FOUND);
        }

        List<EvaluationQuestion> recallQuestions = evaluationQuestionRepository
                .findByEvaluationOrderByQuestionOrderAsc(evaluation)
                .stream()
                .filter(EvaluationQuestion::isRecall)
                .collect(Collectors.toList());

        List<EvaluationResultResponse.QuestionResult> results = new ArrayList<>();
        for (int i = 0; i < recallQuestions.size(); i++) {
            EvaluationQuestion q = recallQuestions.get(i);
            results.add(EvaluationResultResponse.QuestionResult.builder()
                    .id(q.getId())
                    .questionOrder(i + 1)
                    .question(q.getQuestion())
                    .questionType(q.getQuestionType())
                    .modelAnswer(q.getModelAnswer())
                    .userAnswer(q.getUserAnswer())
                    .score(q.getEvaluationScore())
                    .nextReviewDate(q.getNextReviewDate())
                    .choices(parseChoices(q.getChoices(), true))
                    .build());
        }

        return EvaluationResultResponse.builder()
                .evaluationId(evaluation.getId())
                .score(evaluation.getScore())
                .feedback(evaluation.getFeedback())
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

    List<ChoiceItem> parseChoices(String choicesJson, boolean revealCorrect) {
        if (choicesJson == null) return null;
        try {
            JsonNode arr = objectMapper.readTree(choicesJson);
            List<ChoiceItem> list = new ArrayList<>();
            for (JsonNode n : arr) {
                Boolean isCorrect = revealCorrect ? n.path("isCorrect").asBoolean() : null;
                list.add(ChoiceItem.builder()
                        .label(n.path("label").asText())
                        .text(n.path("text").asText())
                        .isCorrect(isCorrect)
                        .build());
            }
            return list;
        } catch (Exception e) {
            log.warn("Failed to parse choices JSON", e);
            return null;
        }
    }

    private String findCorrectLabel(String choicesJson) {
        if (choicesJson == null) return "";
        try {
            JsonNode arr = objectMapper.readTree(choicesJson);
            for (JsonNode n : arr) {
                if (n.path("isCorrect").asBoolean()) {
                    return n.path("label").asText();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to find correct label in choices JSON", e);
        }
        return "";
    }
}
