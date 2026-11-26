package com.kawser.cleanspringbootproject.feedback.domain.model;

public enum NotificationStatus {
    /** Closed with the X: hidden until the next UTC day. */
    DISMISSED,
    /** Answered: never shown again. */
    ANSWERED
}
