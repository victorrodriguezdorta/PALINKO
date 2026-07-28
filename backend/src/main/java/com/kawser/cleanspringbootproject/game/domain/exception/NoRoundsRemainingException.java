package com.kawser.cleanspringbootproject.game.domain.exception;

public class NoRoundsRemainingException extends GameDomainException {

    public NoRoundsRemainingException(String roomCode) {
        super("Room " + roomCode + " has no rounds remaining");
    }
}
