package com.kawser.cleanspringbootproject.game.domain.exception;

import java.util.Map;

public class RoomNotInProgressException extends GameDomainException {

    public RoomNotInProgressException(String roomCode) {
        super("ROOM_NOT_IN_PROGRESS", "Room " + roomCode + " is not in progress", Map.of("roomCode", roomCode));
    }
}
