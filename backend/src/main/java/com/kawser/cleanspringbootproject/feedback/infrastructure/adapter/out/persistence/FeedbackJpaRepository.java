package com.kawser.cleanspringbootproject.feedback.infrastructure.adapter.out.persistence;

import com.kawser.cleanspringbootproject.feedback.domain.model.NotificationTopic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FeedbackJpaRepository extends JpaRepository<FeedbackJpaEntity, Long> {

    Optional<FeedbackJpaEntity> findByUserIdAndTopic(Long userId, NotificationTopic topic);
}
