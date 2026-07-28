package com.kawser.cleanspringbootproject.game.domain.exception;

public class AnswerNotSubmittedException extends GameDomainException {

    public AnswerNotSubmittedException(String playerId) {
        super("Player " + playerId + " has not submitted an answer to cancel");
    }
}
