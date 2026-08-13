package com.kawser.cleanspringbootproject.secretmode.application.port.out;

/**
 * Signals that a {@link SecretQualityChecker} could not produce a judgement
 * at all (network error, non-200 response, malformed reply) as opposed to a
 * genuine negative judgement. Mirrors
 * game.application.port.out.WordRelationCheckException.
 */
public class SecretQualityCheckException extends RuntimeException {

    public SecretQualityCheckException(String message, Throwable cause) {
        super(message, cause);
    }

    public SecretQualityCheckException(String message) {
        super(message);
    }
}
