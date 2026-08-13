package com.kawser.cleanspringbootproject.secretmode.domain.model;

import com.kawser.cleanspringbootproject.secretmode.domain.exception.HintNotAvailableException;
import com.kawser.cleanspringbootproject.secretmode.domain.exception.SecretRoomNotInProgressException;
import com.kawser.cleanspringbootproject.secretmode.domain.service.SecretScoringPolicy;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Root aggregate for a single secret-mode game session: one player against
 * one Concept's hidden quality. Held entirely in memory and discarded once
 * the room is cleaned up, exactly like game.domain.model.Room - but with no
 * turns/phases/infiltrator, since this is always a solo game (see
 * CLAUDE.md-equivalent design notes: a future multiplayer secretmode would
 * extend this aggregate rather than reuse Room/Round, which are shaped
 * around the chain-of-words mechanic).
 */
public class SecretRoom {

    public static final int FAILURES_PER_HINT = 5;

    private final String code;
    private final SecretPlayer player;
    private final Concept concept;
    private final List<WordAttempt> attempts = new ArrayList<>();
    private final Map<String, WordAttempt> attemptsByNormalizedText = new HashMap<>();
    private int totalFailures;
    private int hintsUsed;
    private final List<SolutionAttempt> solutionAttempts = new ArrayList<>();
    private SecretRoomStatus status;
    private Integer finalScore;
    private final Instant createdAt;
    private Instant lastActivityAt;

    private SecretRoom(String code, Concept concept, SecretPlayer player, Instant now) {
        this.code = code;
        this.concept = concept;
        this.player = player;
        this.status = SecretRoomStatus.IN_PROGRESS;
        this.createdAt = now;
        this.lastActivityAt = now;
    }

    public static SecretRoom create(String code, Concept concept, SecretPlayer player, Instant now) {
        return new SecretRoom(code, concept, player, now);
    }

    /**
     * Records a judged word submission. The application layer has already
     * resolved whether it was accepted and by what source (word bank vs AI)
     * before calling here — this method never talks to the AI or the
     * spelling corrector itself, mirroring how Round.submitWord only
     * receives an already-computed WordJudgement.
     *
     * <p>A word already tried before (case/whitespace-insensitively) returns
     * its original WordAttempt unchanged instead of recording a new one: no
     * new AI call, no effect on totalFailures or the eventual score (see
     * requireValidToken's caller for the "don't re-spend AI budget on a
     * repeat" rule this implements).
     */
    public WordAttempt recordWordAttempt(String correctedText, boolean accepted, JudgementSource source, Instant now) {
        requireInProgress();
        String normalized = normalize(correctedText);
        WordAttempt existing = attemptsByNormalizedText.get(normalized);
        if (existing != null) {
            return existing;
        }

        WordAttempt attempt = new WordAttempt(
                UUID.randomUUID().toString(), correctedText,
                accepted ? WordAttemptOutcome.ACCEPTED : WordAttemptOutcome.REJECTED, source, now);
        attempts.add(attempt);
        attemptsByNormalizedText.put(normalized, attempt);
        if (!accepted) {
            totalFailures++;
        }
        touch(now);
        return attempt;
    }

    /**
     * How many hint reveals the player currently has banked: one per every
     * FAILURES_PER_HINT accumulated failures (accumulating across the whole
     * game, never reset by a correct guess), minus however many have
     * already been spent. Derived rather than stored so useHint only ever
     * has to adjust hintsUsed.
     */
    public int hintsAvailable() {
        return Math.max(0, totalFailures / FAILURES_PER_HINT - hintsUsed);
    }

    public int totalFailures() {
        return totalFailures;
    }

    /**
     * Spends one banked hint reveal, recording the given (already-chosen,
     * not-yet-tried) accepted word as an ordinary ACCEPTED attempt tagged
     * source=HINT so it shows up mixed into the player's accepted-words
     * list rather than a separate section. Which word to reveal is decided
     * by the application layer (it needs the full Concept.acceptedWords()
     * filtered by triedWordsLowercase(), which lives outside the domain).
     */
    public WordAttempt useHint(String revealedWord, Instant now) {
        requireInProgress();
        if (hintsAvailable() <= 0) {
            throw new HintNotAvailableException(code);
        }
        hintsUsed++;
        WordAttempt attempt = recordWordAttempt(revealedWord, true, JudgementSource.HINT, now);
        touch(now);
        return attempt;
    }

    public Set<String> triedWordsLowercase() {
        return Collections.unmodifiableSet(attemptsByNormalizedText.keySet());
    }

    /**
     * Records a declared-quality attempt. Only a CORRECT match ends the
     * game (finalScore is computed and the room moves to SOLVED); PARTIAL
     * and INCORRECT are just logged so the player can see their past
     * guesses and keep trying.
     */
    public SolutionAttempt recordSolutionAttempt(
            String formulationText, SolutionMatchLevel matchLevel, SecretScoringPolicy scoringPolicy, Instant now) {
        requireInProgress();
        SolutionAttempt attempt = new SolutionAttempt(UUID.randomUUID().toString(), formulationText, matchLevel, now);
        solutionAttempts.add(attempt);
        if (matchLevel == SolutionMatchLevel.CORRECT) {
            status = SecretRoomStatus.SOLVED;
            finalScore = scoringPolicy.scoreSolved(countAccepted(), countRejected(), hintsUsed);
        }
        touch(now);
        return attempt;
    }

    public void giveUp(SecretScoringPolicy scoringPolicy, Instant now) {
        requireInProgress();
        status = SecretRoomStatus.GAVE_UP;
        finalScore = scoringPolicy.scoreGaveUp(countAccepted(), countRejected(), hintsUsed);
        touch(now);
    }

    /**
     * Tears the room down because its lone player failed to reconnect
     * within the grace window — idempotent, same as Room.close, so a late
     * duplicate sweep call can never downgrade an already-finished game's
     * result back into a "left" state.
     */
    public void close(Instant now) {
        if (status == SecretRoomStatus.SOLVED || status == SecretRoomStatus.GAVE_UP
                || status == SecretRoomStatus.CLOSED) {
            return;
        }
        status = SecretRoomStatus.CLOSED;
        touch(now);
    }

    public void markDisconnected(Instant now) {
        player.markDisconnected(now);
        touch(now);
    }

    public void markReconnected(Instant now) {
        player.markConnected();
        touch(now);
    }

    public boolean isReconnectWindowExpired(Instant now, Duration reconnectWindow) {
        return player.isReconnectWindowExpired(now, reconnectWindow);
    }

    private int countAccepted() {
        return (int) attempts.stream().filter(a -> a.outcome() == WordAttemptOutcome.ACCEPTED).count();
    }

    private int countRejected() {
        return (int) attempts.stream().filter(a -> a.outcome() == WordAttemptOutcome.REJECTED).count();
    }

    private String normalize(String text) {
        return text.trim().toLowerCase();
    }

    private void requireInProgress() {
        if (status != SecretRoomStatus.IN_PROGRESS) {
            throw new SecretRoomNotInProgressException(code);
        }
    }

    private void touch(Instant now) {
        this.lastActivityAt = now;
    }

    public String code() {
        return code;
    }

    public SecretPlayer player() {
        return player;
    }

    public Concept concept() {
        return concept;
    }

    public List<WordAttempt> attempts() {
        return List.copyOf(attempts);
    }

    public int hintsUsed() {
        return hintsUsed;
    }

    public List<SolutionAttempt> solutionAttempts() {
        return List.copyOf(solutionAttempts);
    }

    public SecretRoomStatus status() {
        return status;
    }

    public Integer finalScore() {
        return finalScore;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant lastActivityAt() {
        return lastActivityAt;
    }
}
