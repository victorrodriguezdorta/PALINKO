package com.kawser.cleanspringbootproject.feedback.application.port.out;

import com.kawser.cleanspringbootproject.feedback.domain.model.NotificationRecord;
import com.kawser.cleanspringbootproject.feedback.domain.model.NotificationTopic;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Stores one row per account and notification topic.
 */
public interface FeedbackRepository {

    Optional<NotificationRecord> find(Long userId, NotificationTopic topic);

    void saveDismissal(Long userId, String username, NotificationTopic topic, LocalDate dismissedOn);

    void saveAnswer(Long userId, String username, NotificationTopic topic, String suggestion, String issues);
}
