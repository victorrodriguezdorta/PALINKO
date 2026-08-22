package com.kawser.cleanspringbootproject.user.application.port.in;

public interface RecordDailyCompletionUseCase {

    /**
     * Extends (or restarts) the given account's daily-challenge streak for
     * today's UTC calendar day — see User.withDailyCompletion for the
     * consecutive-day rule. A no-op if the username doesn't match any
     * account, since the caller (a just-finished daily room) has no
     * meaningful way to surface that failure back to the player.
     */
    void recordDailyCompletion(String username);
}
