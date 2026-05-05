package com.ccp.skAI_castle_server.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "evaluations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Evaluation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_topic_id", nullable = false)
    private StudyTopic studyTopic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_session_id")
    private ChatSession chatSession;

    private Integer score;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    @Builder
    private Evaluation(User user, StudyTopic studyTopic, ChatSession chatSession,
                       Integer score, String feedback) {
        this.user = user;
        this.studyTopic = studyTopic;
        this.chatSession = chatSession;
        this.score = score;
        this.feedback = feedback;
    }

    public void updateResult(Integer score, String feedback) {
        this.score = score;
        this.feedback = feedback;
    }
}
