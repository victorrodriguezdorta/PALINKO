package com.kawser.cleanspringbootproject.feedback.domain.model;

import java.time.LocalDate;

/**
 * What an account last did with a given notification topic.
 */
public record NotificationRecord(
        Long userId, NotificationTopic topic, NotificationStatus status, LocalDate dismissedOn) {

    public boolean answered() {
        return status == NotificationStatus.ANSWERED;
    }

    public boolean dismissedOn(LocalDate day) {
        return status == NotificationStatus.DISMISSED && day.equals(dismissedOn);
    }
}
