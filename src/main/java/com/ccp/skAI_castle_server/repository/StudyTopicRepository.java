package com.ccp.skAI_castle_server.repository;

import com.ccp.skAI_castle_server.domain.entity.StudyTopic;
import com.ccp.skAI_castle_server.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudyTopicRepository extends JpaRepository<StudyTopic, Long> {
    List<StudyTopic> findByUserOrderByCreatedAtDesc(User user);
    Optional<StudyTopic> findByIdAndUser(Long id, User user);
}
