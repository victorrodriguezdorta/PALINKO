package com.kawser.cleanspringbootproject.game.domain.exception;

/**
 * Raised when a player submits words faster than WordSubmissionRateLimiter
 * allows, before any AI relatedness call is made for the attempt. Kept
 * distinct from WordComparisonFailedException (which means the AI call
 * itself failed) since the two need different frontend messaging — this one
 * means "slow down", not "something went wrong".
 */
public class WordSubmissionRateLimitedException extends GameDomainException {

    public WordSubmissionRateLimitedException() {
        super("WORD_SUBMISSION_RATE_LIMITED", "Too many word submissions, slow down");
    }
}
