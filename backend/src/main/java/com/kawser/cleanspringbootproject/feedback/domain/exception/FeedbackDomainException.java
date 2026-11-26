package com.kawser.cleanspringbootproject.feedback.domain.exception;

import java.util.Map;

/**
 * Base type for feedback rule violations, rendered with the same
 * {code, message, args} shape as the other modules.
 */
public abstract class FeedbackDomainException extends RuntimeException {

    private final String errorCode;

    protected FeedbackDomainException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String errorCode() {
        return errorCode;
    }

    public Map<String, String> errorArgs() {
        return Map.of();
    }
}
