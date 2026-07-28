package com.kawser.cleanspringbootproject.game.domain.exception;

public class NotEnoughPlayersException extends GameDomainException {

    public NotEnoughPlayersException(int minimumPlayers) {
        super("At least " + minimumPlayers + " players are required to start the game");
    }
}
