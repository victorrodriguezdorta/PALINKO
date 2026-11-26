package com.kawser.cleanspringbootproject.feedback.domain.model;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Decides whether the creator's feedback letter should be shown: only to
 * accounts with more than {@link #MIN_DAILY_GAMES_EXCLUSIVE} daily games,
 * never again once answered, and not again on the same UTC day it was
 * dismissed.
 */
public final class CreatorLetterPolicy {

    public static final int MIN_DAILY_GAMES_EXCLUSIVE = 5;

    private CreatorLetterPolicy() {
    }

    public static boolean shouldShow(int dailyGamesPlayed, Optional<NotificationRecord> record, LocalDate today) {
        if (dailyGamesPlayed <= MIN_DAILY_GAMES_EXCLUSIVE) {
            return false;
        }
        return record.map(existing -> !existing.answered() && !existing.dismissedOn(today)).orElse(true);
    }
}
