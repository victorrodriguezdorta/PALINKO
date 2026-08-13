package com.kawser.cleanspringbootproject.secretmode.domain.exception;

import java.util.Map;

/**
 * Raised when a player tries to submit a word/hint/solution against a room
 * that already ended (SOLVED/GAVE_UP) or was closed for inactivity.
 */
public class SecretRoomNotInProgressException extends SecretGameDomainException {

    public SecretRoomNotInProgressException(String roomCode) {
        super("SECRET_ROOM_NOT_IN_PROGRESS", "Secret room not in progress: " + roomCode,
                Map.of("roomCode", roomCode));
    }
}
