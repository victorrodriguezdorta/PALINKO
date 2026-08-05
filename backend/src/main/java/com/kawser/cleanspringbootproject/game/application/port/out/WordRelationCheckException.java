package com.kawser.cleanspringbootproject.game.application.port.out;

/**
 * Signals that a {@link WordRelationChecker} could not produce a judgement
 * at all (network error, non-200 response, malformed reply) as opposed to a
 * genuine 0% relatedness score. Callers should surface this as a distinct
 * "comparison failed" error rather than silently treating the attempt as
 * unrelated.
 */
public class WordRelationCheckException extends RuntimeException {

    public WordRelationCheckException(String message, Throwable cause) {
        super(message, cause);
    }

    public WordRelationCheckException(String message) {
        super(message);
    }
}
