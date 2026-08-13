package com.kawser.cleanspringbootproject.secretmode.domain.exception;

/**
 * Raised when a player submits words faster than
 * game.application.port.out.WordSubmissionRateLimiter allows (that
 * out-port is reused as-is, see SecretGameApplicationService), before any
 * AI judgement call is made for the attempt. Kept as its own
 * SecretGameDomainException subclass (rather than reusing
 * game.domain.exception.WordSubmissionRateLimitedException) so
 * secretmode's exception handlers, which only catch
 * SecretGameDomainException, see it like any other secretmode error.
 */
public class SecretWordSubmissionRateLimitedException extends SecretGameDomainException {

    public SecretWordSubmissionRateLimitedException() {
        super("WORD_SUBMISSION_RATE_LIMITED", "Too many word submissions, slow down");
    }
}
