package com.kawser.cleanspringbootproject.game.domain.exception;

public class NotHostException extends GameDomainException {

    public NotHostException(String playerId) {
        super("Player " + playerId + " is not the host of this room");
    }
}
