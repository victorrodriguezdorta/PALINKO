package com.kawser.cleanspringbootproject.game.domain.exception;

import java.util.Map;

public class PlayerNotFoundException extends GameDomainException {

    public PlayerNotFoundException(String playerId) {
        super("PLAYER_NOT_FOUND", "Player not found: " + playerId, Map.of("playerId", playerId));
    }
}
