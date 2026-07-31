package com.kawser.cleanspringbootproject.game.domain.exception;

import java.util.Map;

public class CannotKickHostException extends GameDomainException {

    public CannotKickHostException(String playerId) {
        super("CANNOT_KICK_HOST", "Player " + playerId + " is the host and cannot be kicked", Map.of("playerId", playerId));
    }
}
