package com.kawser.cleanspringbootproject.secretmode.domain.exception;

import java.util.Map;

/**
 * Raised when the player asks for a hint before totalFailures/5 has exceeded
 * hintsUsed (see SecretRoom.hintsAvailable).
 */
public class HintNotAvailableException extends SecretGameDomainException {

    public HintNotAvailableException(String roomCode) {
        super("HINT_NOT_AVAILABLE", "No hint available yet in room: " + roomCode, Map.of("roomCode", roomCode));
    }
}
