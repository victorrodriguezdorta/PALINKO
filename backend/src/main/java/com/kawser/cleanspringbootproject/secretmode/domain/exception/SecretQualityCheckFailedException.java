package com.kawser.cleanspringbootproject.secretmode.domain.exception;

/**
 * Surfaced to the client when the AI judge call itself failed (missing key,
 * network error, malformed response) — as opposed to the AI successfully
 * judging the word/formulation and rejecting it. Mirrors
 * game.domain.exception.WordComparisonFailedException.
 */
public class SecretQualityCheckFailedException extends SecretGameDomainException {

    public SecretQualityCheckFailedException() {
        super("SECRET_QUALITY_CHECK_FAILED", "Could not judge the submission against the secret quality right now");
    }
}
