package com.kawser.cleanspringbootproject.game.domain.exception;

import com.kawser.cleanspringbootproject.game.domain.model.RoundPhase;

import java.util.Map;

public class WrongPhaseException extends GameDomainException {

    public WrongPhaseException(RoundPhase expected, RoundPhase actual) {
        super("WRONG_PHASE", "Expected round phase " + expected + " but round is in " + actual,
                Map.of("expected", expected.name(), "actual", actual.name()));
    }
}
