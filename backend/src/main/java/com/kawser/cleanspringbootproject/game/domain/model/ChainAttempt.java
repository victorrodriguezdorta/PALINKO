package com.kawser.cleanspringbootproject.game.domain.model;

import java.util.UUID;

/**
 * One turn's worth of history in the chain: whatever the player submitted
 * (or a skipped turn on timeout), always kept in the log for transparency
 * even when REJECTED — only ACCEPTED attempts move the chain forward.
 * relatednessToTarget/reachedTarget are only ever populated for ACCEPTED
 * attempts, since a rejected word never advances toward anyone's target.
 * phaseIndex (0-based) records which multi-phase segment of the game this
 * attempt belongs to, since the attempt log spans the whole game rather
 * than being reset per phase.
 */
public record ChainAttempt(
        String id,
        String authorPlayerId,
        int turnNumber,
        String text,
        AttemptOutcome outcome,
        int relatednessToPrevious,
        String justification,
        Integer relatednessToTarget,
        boolean reachedTarget,
        int phaseIndex) {

    public ChainAttempt {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (authorPlayerId == null || authorPlayerId.isBlank()) {
            throw new IllegalArgumentException("authorPlayerId must not be blank");
        }
        if (turnNumber < 1) {
            throw new IllegalArgumentException("turnNumber must be at least 1");
        }
        if (outcome != AttemptOutcome.SKIPPED && (text == null || text.isBlank())) {
            throw new IllegalArgumentException("text is required unless the turn was skipped");
        }
        if (outcome != AttemptOutcome.ACCEPTED && (relatednessToTarget != null || reachedTarget)) {
            throw new IllegalArgumentException("relatednessToTarget/reachedTarget only apply to ACCEPTED attempts");
        }
        if (phaseIndex < 0) {
            throw new IllegalArgumentException("phaseIndex must not be negative");
        }
    }

    public static ChainAttempt accepted(
            String authorPlayerId, int turnNumber, String text,
            int relatednessToPrevious, String justification,
            int relatednessToTarget, boolean reachedTarget, int phaseIndex) {
        return new ChainAttempt(
                UUID.randomUUID().toString(), authorPlayerId, turnNumber, text,
                AttemptOutcome.ACCEPTED, relatednessToPrevious, justification,
                relatednessToTarget, reachedTarget, phaseIndex);
    }

    public static ChainAttempt rejected(
            String authorPlayerId, int turnNumber, String text,
            int relatednessToPrevious, String justification, int phaseIndex) {
        return new ChainAttempt(
                UUID.randomUUID().toString(), authorPlayerId, turnNumber, text,
                AttemptOutcome.REJECTED, relatednessToPrevious, justification, null, false, phaseIndex);
    }

    public static ChainAttempt skipped(String authorPlayerId, int turnNumber, int phaseIndex) {
        return new ChainAttempt(
                UUID.randomUUID().toString(), authorPlayerId, turnNumber, "",
                AttemptOutcome.SKIPPED, 0, null, null, false, phaseIndex);
    }
}
