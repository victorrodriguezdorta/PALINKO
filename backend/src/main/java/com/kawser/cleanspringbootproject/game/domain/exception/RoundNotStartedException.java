package com.kawser.cleanspringbootproject.game.domain.exception;

import java.util.Map;

public class RoundNotStartedException extends GameDomainException {

    public RoundNotStartedException(String roomCode) {
        super("ROUND_NOT_STARTED", "Room " + roomCode + " has no round in progress", Map.of("roomCode", roomCode));
    }
}
