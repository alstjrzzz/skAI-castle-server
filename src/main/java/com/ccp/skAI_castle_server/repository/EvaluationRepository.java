package com.ccp.skAI_castle_server.repository;

import com.ccp.skAI_castle_server.domain.entity.ChatSession;
import com.ccp.skAI_castle_server.domain.entity.Evaluation;
import com.ccp.skAI_castle_server.domain.entity.StudyTopic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {
    Optional<Evaluation> findByChatSession(ChatSession chatSession);
    List<Evaluation> findByStudyTopicOrderByCreatedAtAsc(StudyTopic studyTopic);
}
