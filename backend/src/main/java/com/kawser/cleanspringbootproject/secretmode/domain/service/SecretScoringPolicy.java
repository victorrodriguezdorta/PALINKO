package com.kawser.cleanspringbootproject.secretmode.domain.service;

/**
 * The tunable half of secretmode's ruleset, isolated from SecretRoom's state
 * machine — same split as game.domain.service.ScoringPolicy/Round. Takes raw
 * counts rather than the SecretRoom itself so it stays trivially testable
 * and has no dependency back onto domain.model.
 */
public interface SecretScoringPolicy {

    int scoreSolved(int acceptedAttempts, int rejectedAttempts, int hintsUsed);

    int scoreGaveUp(int acceptedAttempts, int rejectedAttempts, int hintsUsed);
}
