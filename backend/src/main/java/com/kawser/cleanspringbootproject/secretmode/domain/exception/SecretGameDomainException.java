package com.kawser.cleanspringbootproject.secretmode.domain.exception;

import java.util.Map;

/**
 * Base type for every rule violation raised by the secretmode domain model.
 * Mirrors game.domain.exception.GameDomainException exactly (errorCode +
 * errorArgs the frontend translates) but is kept as its own hierarchy so
 * secretmode's REST/STOMP exception handlers can catch only their own
 * exceptions without reaching into the original game module.
 */
public abstract class SecretGameDomainException extends RuntimeException {

    private final String errorCode;
    private final Map<String, String> errorArgs;

    protected SecretGameDomainException(String errorCode, String message) {
        this(errorCode, message, Map.of());
    }

    protected SecretGameDomainException(String errorCode, String message, Map<String, String> errorArgs) {
        super(message);
        this.errorCode = errorCode;
        this.errorArgs = errorArgs;
    }

    public String errorCode() {
        return errorCode;
    }

    public Map<String, String> errorArgs() {
        return errorArgs;
    }
}
