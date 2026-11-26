package com.kawser.cleanspringbootproject.feedback.infrastructure.adapter.out.persistence;

import com.kawser.cleanspringbootproject.feedback.application.port.out.FeedbackRepository;
import com.kawser.cleanspringbootproject.feedback.domain.model.NotificationRecord;
import com.kawser.cleanspringbootproject.feedback.domain.model.NotificationTopic;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public class JpaFeedbackRepository implements FeedbackRepository {

    private final FeedbackJpaRepository jpaRepository;

    public JpaFeedbackRepository(FeedbackJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<NotificationRecord> find(Long userId, NotificationTopic topic) {
        return jpaRepository.findByUserIdAndTopic(userId, topic)
                .map(entity -> new NotificationRecord(
                        entity.getUserId(), entity.getTopic(), entity.getStatus(), entity.getDismissedOn()));
    }

    @Override
    public void saveDismissal(Long userId, String username, NotificationTopic topic, LocalDate dismissedOn) {
        Instant now = Instant.now();
        FeedbackJpaEntity entity = findOrCreate(userId, topic, now);
        entity.markDismissed(username, dismissedOn, now);
        jpaRepository.save(entity);
    }

    @Override
    public void saveAnswer(Long userId, String username, NotificationTopic topic, String suggestion, String issues) {
        Instant now = Instant.now();
        FeedbackJpaEntity entity = findOrCreate(userId, topic, now);
        entity.markAnswered(username, suggestion, issues, now);
        jpaRepository.save(entity);
    }

    private FeedbackJpaEntity findOrCreate(Long userId, NotificationTopic topic, Instant now) {
        return jpaRepository.findByUserIdAndTopic(userId, topic)
                .orElseGet(() -> new FeedbackJpaEntity(userId, topic, now));
    }
}
