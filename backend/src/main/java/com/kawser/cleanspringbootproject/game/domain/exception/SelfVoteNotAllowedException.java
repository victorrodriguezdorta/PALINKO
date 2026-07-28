package com.kawser.cleanspringbootproject.game.domain.exception;

public class SelfVoteNotAllowedException extends GameDomainException {

    public SelfVoteNotAllowedException(String playerId) {
        super("Player " + playerId + " cannot vote for their own answer");
    }
}
