package com.ccp.skAI_castle_server.domain.entity;

import com.ccp.skAI_castle_server.domain.ChatSessionStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "chat_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatSession extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_topic_id", nullable = false)
    private StudyTopic studyTopic;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatSessionStatus status;

    @Builder
    private ChatSession(User user, StudyTopic studyTopic, ChatSessionStatus status) {
        this.user = user;
        this.studyTopic = studyTopic;
        this.status = status != null ? status : ChatSessionStatus.ACTIVE;
    }

    public void close() {
        this.status = ChatSessionStatus.CLOSED;
    }
}
