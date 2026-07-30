package com.kawser.cleanspringbootproject.game.domain.exception;

import java.util.Map;

public class DuplicatePlayerNameException extends GameDomainException {

    public DuplicatePlayerNameException(String name) {
        super("DUPLICATE_PLAYER_NAME", "Player name already taken in this room: " + name, Map.of("name", name));
    }
}
