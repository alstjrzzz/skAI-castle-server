package com.ccp.skAI_castle_server.repository;

import com.ccp.skAI_castle_server.domain.entity.ChatSession;
import com.ccp.skAI_castle_server.domain.entity.StudyTopic;
import com.ccp.skAI_castle_server.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    Optional<ChatSession> findByIdAndUser(Long id, User user);
    List<ChatSession> findByStudyTopicOrderByCreatedAtAsc(StudyTopic studyTopic);
}
