package com.kawser.cleanspringbootproject.game.domain.exception;

import com.kawser.cleanspringbootproject.game.domain.model.RoundPhase;

public class WrongPhaseException extends GameDomainException {

    public WrongPhaseException(RoundPhase expected, RoundPhase actual) {
        super("Expected round phase " + expected + " but round is in " + actual);
    }
}
