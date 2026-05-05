package com.ccp.skAI_castle_server.service;

import com.ccp.skAI_castle_server.domain.entity.ChatSession;
import com.ccp.skAI_castle_server.domain.entity.Evaluation;
import com.ccp.skAI_castle_server.domain.entity.StudyTopic;
import com.ccp.skAI_castle_server.domain.entity.User;
import com.ccp.skAI_castle_server.dto.response.*;
import com.ccp.skAI_castle_server.exception.ApiException;
import com.ccp.skAI_castle_server.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.ccp.skAI_castle_server.dto.response.ApiResultCode.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudyTopicService {

    private final StudyTopicRepository studyTopicRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final EvaluationRepository evaluationRepository;
    private final EvaluationQuestionRepository evaluationQuestionRepository;
    private final UserRepository userRepository;
    private final LlmService llmService;
    private final ObjectMapper objectMapper;

    @Transactional
    public StudyTopicResponse createTopic(String title, User user) {
        StudyTopic topic = StudyTopic.builder()
                .user(user)
                .title(title)
                .build();
        studyTopicRepository.save(topic);
        return toResponse(topic);
    }

    @Transactional(readOnly = true)
    public List<StudyTopicSummaryResponse> getMyTopics(User user) {
        return studyTopicRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(t -> StudyTopicSummaryResponse.builder()
                        .id(t.getId())
                        .title(t.getTitle())
                        .status(t.getStatus().name())
                        .hasOutline(t.getOutline() != null)
                        .createdAt(t.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StudyTopicResponse getTopic(Long topicId, User user) {
        StudyTopic topic = studyTopicRepository.findByIdAndUser(topicId, user)
                .orElseThrow(() -> new ApiException(TOPIC_NOT_FOUND));
        return toResponse(topic);
    }

    @Transactional
    public OutlineDto generateOutline(Long topicId, User user) {
        StudyTopic topic = studyTopicRepository.findByIdAndUser(topicId, user)
                .orElseThrow(() -> new ApiException(TOPIC_NOT_FOUND));

        if (topic.getOutline() != null) {
            throw new ApiException(OUTLINE_ALREADY_EXISTS);
        }

        OutlineDto outlineDto = llmService.generateOutline(topic.getTitle());

        try {
            String outlineJson = objectMapper.writeValueAsString(outlineDto);
            topic.updateOutline(outlineJson);
        } catch (Exception e) {
            throw new ApiException(EXTERNAL_API_ERROR, e);
        }

        return outlineDto;
    }

    @Transactional(readOnly = true)
    public TopicHistoryResponse getTopicHistory(Long topicId, User user) {
        StudyTopic topic = studyTopicRepository.findByIdAndUser(topicId, user)
                .orElseThrow(() -> new ApiException(TOPIC_NOT_FOUND));

        List<ChatSession> sessions = chatSessionRepository.findByStudyTopicOrderByCreatedAtAsc(topic);
        LocalDate today = LocalDate.now();

        List<TopicHistoryResponse.SessionHistoryItem> sessionItems = sessions.stream()
                .map(session -> {
                    Evaluation eval = evaluationRepository.findByChatSession(session).orElse(null);
                    TopicHistoryResponse.SessionHistoryItem.EvaluationSummary evalSummary = null;

                    if (eval != null) {
                        long questionCount = evaluationQuestionRepository.countByEvaluation(eval);
                        long pendingReviewCount = evaluationQuestionRepository.countPendingReviews(eval, today);
                        evalSummary = TopicHistoryResponse.SessionHistoryItem.EvaluationSummary.builder()
                                .evaluationId(eval.getId())
                                .score(eval.getScore())
                                .feedback(eval.getFeedback())
                                .questionCount((int) questionCount)
                                .pendingReviewCount(pendingReviewCount)
                                .build();
                    }

                    return TopicHistoryResponse.SessionHistoryItem.builder()
                            .sessionId(session.getId())
                            .status(session.getStatus().name())
                            .createdAt(session.getCreatedAt())
                            .evaluation(evalSummary)
                            .build();
                })
                .collect(Collectors.toList());

        return TopicHistoryResponse.builder()
                .topicId(topic.getId())
                .title(topic.getTitle())
                .status(topic.getStatus().name())
                .outline(parseOutline(topic.getOutline()))
                .sessions(sessionItems)
                .build();
    }

    public User loadUser(String uuid) {
        return userRepository.findByUuid(UUID.fromString(uuid))
                .orElseThrow(() -> new ApiException(USER_NOT_FOUND));
    }

    private StudyTopicResponse toResponse(StudyTopic topic) {
        return StudyTopicResponse.builder()
                .id(topic.getId())
                .title(topic.getTitle())
                .outline(parseOutline(topic.getOutline()))
                .status(topic.getStatus().name())
                .createdAt(topic.getCreatedAt())
                .updatedAt(topic.getUpdatedAt())
                .build();
    }

    private OutlineDto parseOutline(String outlineJson) {
        if (outlineJson == null) return null;
        try {
            return objectMapper.readValue(outlineJson, OutlineDto.class);
        } catch (Exception e) {
            log.warn("Failed to parse outline JSON", e);
            return null;
        }
    }
}
