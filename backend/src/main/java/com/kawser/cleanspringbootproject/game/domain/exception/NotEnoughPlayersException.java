package com.kawser.cleanspringbootproject.game.domain.exception;

import java.util.Map;

public class NotEnoughPlayersException extends GameDomainException {

    public NotEnoughPlayersException(int minimumPlayers) {
        super("NOT_ENOUGH_PLAYERS", "At least " + minimumPlayers + " players are required to start the game",
                Map.of("minimumPlayers", String.valueOf(minimumPlayers)));
    }
}
