package com.ccp.skAI_castle_server.domain.entity;

import com.ccp.skAI_castle_server.domain.ChatMessageRole;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "chat_messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ChatSession session;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatMessageRole role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private Integer turnNumber;

    @Builder
    private ChatMessage(ChatSession session, ChatMessageRole role, String content, Integer turnNumber) {
        this.session = session;
        this.role = role;
        this.content = content;
        this.turnNumber = turnNumber;
    }
}
