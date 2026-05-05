package com.ccp.skAI_castle_server.repository;

import com.ccp.skAI_castle_server.domain.ChatMessageRole;
import com.ccp.skAI_castle_server.domain.entity.ChatMessage;
import com.ccp.skAI_castle_server.domain.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findBySessionOrderByTurnNumberAscIdAsc(ChatSession session);
    long countBySessionAndRole(ChatSession session, ChatMessageRole role);
}
