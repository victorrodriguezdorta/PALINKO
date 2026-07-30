package com.kawser.cleanspringbootproject.game.domain.exception;

import java.util.Map;

public class NotHostException extends GameDomainException {

    public NotHostException(String playerId) {
        super("NOT_HOST", "Player " + playerId + " is not the host of this room", Map.of("playerId", playerId));
    }
}
