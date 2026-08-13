package com.kawser.cleanspringbootproject.secretmode.domain.exception;

import java.util.Map;

public class SecretPlayerNotFoundException extends SecretGameDomainException {

    public SecretPlayerNotFoundException(String playerId) {
        super("SECRET_PLAYER_NOT_FOUND", "Secret player not found: " + playerId, Map.of("playerId", playerId));
    }
}
