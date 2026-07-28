package com.kawser.cleanspringbootproject.game.domain.exception;

public class InvalidPlayerNameException extends GameDomainException {

    public InvalidPlayerNameException(String name) {
        super("Invalid player name: '" + name + "'");
    }
}
