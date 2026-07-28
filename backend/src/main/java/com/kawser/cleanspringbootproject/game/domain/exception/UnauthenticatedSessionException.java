package com.kawser.cleanspringbootproject.game.domain.exception;

public class UnauthenticatedSessionException extends GameDomainException {

    public UnauthenticatedSessionException() {
        super("No authenticated session for this connection");
    }
}
