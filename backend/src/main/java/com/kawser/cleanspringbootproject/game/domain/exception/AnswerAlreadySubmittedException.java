package com.kawser.cleanspringbootproject.game.domain.exception;

public class AnswerAlreadySubmittedException extends GameDomainException {

    public AnswerAlreadySubmittedException(String playerId) {
        super("Player " + playerId + " already submitted an answer this round");
    }
}
