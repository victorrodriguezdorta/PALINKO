package com.kawser.cleanspringbootproject.feedback.infrastructure.adapter.out.persistence;

import com.kawser.cleanspringbootproject.feedback.domain.model.NotificationStatus;
import com.kawser.cleanspringbootproject.feedback.domain.model.NotificationTopic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "feedback")
public class FeedbackJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationTopic topic;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status;

    @Column(columnDefinition = "TEXT")
    private String suggestion;

    @Column(columnDefinition = "TEXT")
    private String issues;

    @Column(name = "dismissed_on")
    private LocalDate dismissedOn;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected FeedbackJpaEntity() {
    }

    public FeedbackJpaEntity(Long userId, NotificationTopic topic, Instant now) {
        this.userId = userId;
        this.topic = topic;
        this.createdAt = now;
    }

    public void markDismissed(String username, LocalDate day, Instant now) {
        this.username = username;
        this.status = NotificationStatus.DISMISSED;
        this.dismissedOn = day;
        this.updatedAt = now;
    }

    public void markAnswered(String username, String suggestion, String issues, Instant now) {
        this.username = username;
        this.status = NotificationStatus.ANSWERED;
        this.suggestion = suggestion;
        this.issues = issues;
        this.updatedAt = now;
    }

    public Long getUserId() {
        return userId;
    }

    public NotificationTopic getTopic() {
        return topic;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public LocalDate getDismissedOn() {
        return dismissedOn;
    }
}
