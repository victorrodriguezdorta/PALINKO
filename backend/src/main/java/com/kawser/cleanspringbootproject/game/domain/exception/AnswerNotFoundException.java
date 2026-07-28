package com.kawser.cleanspringbootproject.game.domain.exception;

public class AnswerNotFoundException extends GameDomainException {

    public AnswerNotFoundException(String answerId) {
        super("Answer not found: " + answerId);
    }
}
