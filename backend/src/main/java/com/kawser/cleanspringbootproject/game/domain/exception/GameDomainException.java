package com.kawser.cleanspringbootproject.game.domain.exception;

/**
 * Base type for every rule violation raised by the game domain model.
 * Infrastructure adapters catch this (not the individual subclasses one by
 * one) when they only need a generic "bad request" mapping.
 */
public abstract class GameDomainException extends RuntimeException {

    protected GameDomainException(String message) {
        super(message);
    }
}
