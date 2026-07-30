package com.kawser.cleanspringbootproject.game.domain.exception;

import java.util.Map;

public class RoomNotJoinableException extends GameDomainException {

    public RoomNotJoinableException(String roomCode) {
        super("ROOM_NOT_JOINABLE", "Room " + roomCode + " is not accepting new players", Map.of("roomCode", roomCode));
    }
}
