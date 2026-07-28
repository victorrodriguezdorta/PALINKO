package com.kawser.cleanspringbootproject.game.domain.exception;

public class VoteAlreadySubmittedException extends GameDomainException {

    public VoteAlreadySubmittedException(String playerId) {
        super("Player " + playerId + " already voted this round");
    }
}
