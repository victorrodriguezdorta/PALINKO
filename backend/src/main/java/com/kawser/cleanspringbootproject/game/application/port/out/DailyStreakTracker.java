package com.kawser.cleanspringbootproject.game.application.port.out;

/**
 * Lets the game module report a completed daily challenge without depending
 * on the user module directly — implemented in user.infrastructure by
 * delegating to RecordDailyCompletionUseCase, mirroring how every other
 * out-port here is implemented by an adapter outside this module's own
 * package tree.
 */
public interface DailyStreakTracker {

    void recordDailyCompletion(String username);
}
