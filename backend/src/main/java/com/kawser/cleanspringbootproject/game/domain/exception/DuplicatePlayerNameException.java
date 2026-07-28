package com.kawser.cleanspringbootproject.game.domain.exception;

public class DuplicatePlayerNameException extends GameDomainException {

    public DuplicatePlayerNameException(String name) {
        super("Player name already taken in this room: " + name);
    }
}
