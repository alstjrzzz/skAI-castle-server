package com.ccp.skAI_castle_server.domain.entity;

import com.ccp.skAI_castle_server.domain.StudyTopicStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "study_topics")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudyTopic extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String title;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String outline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StudyTopicStatus status;

    @Builder
    private StudyTopic(User user, String title, String outline, StudyTopicStatus status) {
        this.user = user;
        this.title = title;
        this.outline = outline;
        this.status = status != null ? status : StudyTopicStatus.IN_PROGRESS;
    }

    public void updateOutline(String outline) {
        this.outline = outline;
    }
}
