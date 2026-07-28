package com.kawser.cleanspringbootproject.game.application.port.out;

import com.kawser.cleanspringbootproject.game.domain.model.RoundPhase;

import java.time.Instant;

/**
 * Schedules the forced phase-advance for a room's current round. Kept as a
 * port (rather than calling Spring's TaskScheduler directly from
 * application) so use-case tests can substitute a fake that fires
 * synchronously instead of waiting on real time.
 */
public interface PhaseScheduler {

    /**
     * Schedules a callback for {@code fireAt}. roundNumber/expectedPhase are
     * captured by the caller so that, once the timer fires, the use case can
     * detect a phase that has already moved on (by other means) and treat
     * the callback as a no-op instead of needing explicit cancellation.
     */
    void scheduleAdvance(String roomCode, int roundNumber, RoundPhase expectedPhase, Instant fireAt);

    void cancel(String roomCode);
}
