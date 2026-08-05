package com.kawser.cleanspringbootproject.game.infrastructure.adapter.out.ratelimit;

import com.kawser.cleanspringbootproject.game.application.port.out.WordSubmissionRateLimiter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caps each player to a fixed number of word submissions per fixed
 * burst-sized time window (e.g. 6 per 10 seconds), keyed purely by
 * playerId. Round.requireCurrentTurn already stops a player from submitting
 * twice within the *same* turn, so this exists only to stop a player from
 * immediately re-triggering a fresh turn over and over — the one path where
 * that's possible without any other player's cooperation is a single-player
 * room (see daily challenges), where the turn always comes right back to
 * you. Each accepted or rejected submitWord call still costs at least one
 * AI relatedness check, so this is the last guard before that spend.
 *
 * <p>A fixed window (rather than a sliding one or a token bucket) is
 * deliberately good enough here: the failure mode being defended against is
 * sustained rapid-fire spam, not a single burst that straddles a window
 * boundary, and counters are naturally bounded since disconnected/finished
 * players simply stop being touched (same reasoning as RateLimitFilter's
 * per-day keys, but per-window instead of per-day so entries turn over
 * quickly rather than accumulating for a whole day).
 *
 * <p>Each player's Window is checked and updated under its own monitor
 * rather than with atomics: the reset-then-increment sequence needs to
 * happen as one step, and per-player contention is low enough (one player
 * can only ever be mid-turn in one room at a time) that a plain
 * synchronized block is simpler than a lock-free CAS loop for the same
 * correctness.
 */
@Component
public class InMemoryWordSubmissionRateLimiter implements WordSubmissionRateLimiter {

    private final int maxSubmissionsPerWindow;
    private final long windowMillis;
    private final Clock clock;
    private final ConcurrentHashMap<String, Window> windowsByPlayerId = new ConcurrentHashMap<>();

    @Autowired
    public InMemoryWordSubmissionRateLimiter(
            @Value("${game.rate-limit.word-submission.max-per-window}") int maxSubmissionsPerWindow,
            @Value("${game.rate-limit.word-submission.window-seconds}") long windowSeconds) {
        this(maxSubmissionsPerWindow, windowSeconds, Clock.systemUTC());
    }

    InMemoryWordSubmissionRateLimiter(int maxSubmissionsPerWindow, long windowSeconds, Clock clock) {
        this.maxSubmissionsPerWindow = maxSubmissionsPerWindow;
        this.windowMillis = windowSeconds * 1000;
        this.clock = clock;
    }

    @Override
    public boolean tryAcquire(String playerId) {
        Window window = windowsByPlayerId.computeIfAbsent(playerId, ignored -> new Window());
        synchronized (window) {
            long now = clock.millis();
            if (now - window.windowStartMillis >= windowMillis) {
                window.windowStartMillis = now;
                window.count = 0;
            }
            window.count++;
            return window.count <= maxSubmissionsPerWindow;
        }
    }

    private static final class Window {
        private long windowStartMillis = 0;
        private int count = 0;
    }
}
