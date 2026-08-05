package com.kawser.cleanspringbootproject.game.application.port.out;

/**
 * Guards how often a single player may submit a word, independent of turn
 * order: Round already forces one submission per turn, but nothing else
 * stops a player from immediately re-triggering a fresh turn in a
 * single-player room (see daily challenges), so this is the last line of
 * defense against a player burning through the AI relatedness-check budget.
 */
public interface WordSubmissionRateLimiter {

    /**
     * @return true if playerId may submit a word right now, consuming one
     * unit of their allowance as a side effect. Returning false must not
     * consume any allowance.
     */
    boolean tryAcquire(String playerId);
}
