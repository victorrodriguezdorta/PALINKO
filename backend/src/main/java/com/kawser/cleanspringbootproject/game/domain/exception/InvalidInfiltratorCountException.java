package com.kawser.cleanspringbootproject.game.domain.exception;

import java.util.Map;

public class InvalidInfiltratorCountException extends GameDomainException {

    public InvalidInfiltratorCountException(int requested, int maxAllowed) {
        super("INVALID_INFILTRATOR_COUNT",
                "Requested " + requested + " infiltrators but at most " + maxAllowed + " are allowed",
                Map.of("requested", String.valueOf(requested), "maxAllowed", String.valueOf(maxAllowed)));
    }
}
