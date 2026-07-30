package com.kawser.cleanspringbootproject.game.domain.exception;

import java.util.Map;

public class NotYourTurnException extends GameDomainException {

    public NotYourTurnException(String playerId) {
        super("NOT_YOUR_TURN", "It is not player " + playerId + "'s turn", Map.of("playerId", playerId));
    }
}
