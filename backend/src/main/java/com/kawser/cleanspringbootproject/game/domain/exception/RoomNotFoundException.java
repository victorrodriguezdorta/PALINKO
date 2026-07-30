package com.kawser.cleanspringbootproject.game.domain.exception;

import java.util.Map;

public class RoomNotFoundException extends GameDomainException {

    public RoomNotFoundException(String roomCode) {
        super("ROOM_NOT_FOUND", "Room not found: " + roomCode, Map.of("roomCode", roomCode));
    }
}
