package com.kawser.cleanspringbootproject.game.domain.exception;

import java.util.Map;

public class RoomNotFinishedException extends GameDomainException {

    public RoomNotFinishedException(String roomCode) {
        super("ROOM_NOT_FINISHED", "Room " + roomCode + " has not finished yet", Map.of("roomCode", roomCode));
    }
}
