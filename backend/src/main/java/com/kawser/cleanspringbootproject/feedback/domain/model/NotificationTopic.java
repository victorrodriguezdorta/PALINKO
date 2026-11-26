package com.kawser.cleanspringbootproject.feedback.domain.model;

/**
 * Identifies which card/notification a feedback row belongs to. Persisted
 * by name, so never rename an existing constant: add a new one instead.
 */
public enum NotificationTopic {
    /** Letter from the creator asking frequent players for feedback. */
    CREATOR_LETTER
}
