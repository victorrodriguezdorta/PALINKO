package com.kawser.cleanspringbootproject.game.domain.exception;

public class PlayerNotFoundException extends GameDomainException {

    public PlayerNotFoundException(String playerId) {
        super("Player not found: " + playerId);
    }
}
