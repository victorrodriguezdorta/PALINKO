package com.kawser.cleanspringbootproject.game.domain.exception;

import java.util.Map;

public class RoomFullException extends GameDomainException {

    public RoomFullException(String roomCode) {
        super("ROOM_FULL", "Room " + roomCode + " is full", Map.of("roomCode", roomCode));
    }
}
