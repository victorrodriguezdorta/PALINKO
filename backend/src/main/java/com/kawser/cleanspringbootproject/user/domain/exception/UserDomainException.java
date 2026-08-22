package com.kawser.cleanspringbootproject.user.domain.exception;

import java.util.Map;

/**
 * Base type for every rule violation raised by the user domain model —
 * mirrors game.domain.exception.GameDomainException so the REST adapter can
 * translate errorCode + errorArgs into the same {code, message, args} shape
 * the frontend already knows how to render (see UserErrorMessage /
 * translateError()).
 */
public abstract class UserDomainException extends RuntimeException {

    private final String errorCode;
    private final Map<String, String> errorArgs;

    protected UserDomainException(String errorCode, String message) {
        this(errorCode, message, Map.of());
    }

    protected UserDomainException(String errorCode, String message, Map<String, String> errorArgs) {
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
