package com.kawser.cleanspringbootproject.game.domain.exception;

import java.util.Map;

/**
 * Base type for every rule violation raised by the game domain model.
 * Infrastructure adapters catch this (not the individual subclasses one by
 * one) when they only need a generic "bad request" mapping. Alongside the
 * free-text message (kept for server logs), every subclass carries a
 * stable errorCode plus any dynamic values as named errorArgs — that's
 * what the frontend actually renders, translated into the room/viewer's
 * own language, instead of depending on this class's English message text.
 */
public abstract class GameDomainException extends RuntimeException {

    private final String errorCode;
    private final Map<String, String> errorArgs;

    protected GameDomainException(String errorCode, String message) {
        this(errorCode, message, Map.of());
    }

    protected GameDomainException(String errorCode, String message, Map<String, String> errorArgs) {
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
