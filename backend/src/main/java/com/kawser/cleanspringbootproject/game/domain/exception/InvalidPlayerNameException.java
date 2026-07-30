package com.kawser.cleanspringbootproject.game.domain.exception;

import java.util.Map;

public class InvalidPlayerNameException extends GameDomainException {

    public InvalidPlayerNameException(String name) {
        super("INVALID_PLAYER_NAME", "Invalid player name: '" + name + "'", Map.of("name", name));
    }
}
