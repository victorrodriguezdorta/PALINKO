package com.kawser.cleanspringbootproject.secretmode.domain.exception;

import java.util.Map;

public class SecretRoomNotFoundException extends SecretGameDomainException {

    public SecretRoomNotFoundException(String roomCode) {
        super("SECRET_ROOM_NOT_FOUND", "Secret room not found: " + roomCode, Map.of("roomCode", roomCode));
    }
}
