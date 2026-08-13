package com.kawser.cleanspringbootproject.secretmode.domain.service;

/**
 * Rewards efficiency the same way game.domain.service.DefaultScoringPolicy
 * does: every attempt (even an accepted one) shaves a little off the score
 * so solving in fewer total words beats solving in more, a rejected attempt
 * costs noticeably more than an accepted one, and a used hint costs the
 * most of all. Giving up starts from half the base, since the game was
 * never actually solved.
 */
public class DefaultSecretScoringPolicy implements SecretScoringPolicy {

    public static final int BASE_POINTS = 100;
    public static final int GIVE_UP_BASE_POINTS = BASE_POINTS / 2;
    public static final int POINTS_PER_REJECTED_ATTEMPT = -4;
    public static final int POINTS_PER_ACCEPTED_ATTEMPT = -1;
    public static final int POINTS_PER_HINT_USED = -15;
    public static final int MIN_SCORE = 0;

    @Override
    public int scoreSolved(int acceptedAttempts, int rejectedAttempts, int hintsUsed) {
        return score(BASE_POINTS, acceptedAttempts, rejectedAttempts, hintsUsed);
    }

    @Override
    public int scoreGaveUp(int acceptedAttempts, int rejectedAttempts, int hintsUsed) {
        return score(GIVE_UP_BASE_POINTS, acceptedAttempts, rejectedAttempts, hintsUsed);
    }

    private int score(int basePoints, int acceptedAttempts, int rejectedAttempts, int hintsUsed) {
        int raw = basePoints
                + rejectedAttempts * POINTS_PER_REJECTED_ATTEMPT
                + acceptedAttempts * POINTS_PER_ACCEPTED_ATTEMPT
                + hintsUsed * POINTS_PER_HINT_USED;
        return Math.max(MIN_SCORE, raw);
    }
}
