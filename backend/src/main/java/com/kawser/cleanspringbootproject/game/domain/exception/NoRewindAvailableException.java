package com.kawser.cleanspringbootproject.game.domain.exception;

import java.util.Map;

public class NoRewindAvailableException extends GameDomainException {

    public NoRewindAvailableException(String playerId) {
        super("NO_REWIND_AVAILABLE", "Player " + playerId + " has no rewind available", Map.of("playerId", playerId));
    }
}
