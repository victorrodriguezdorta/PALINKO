package com.kawser.cleanspringbootproject.game.domain.exception;

import java.util.Map;

public class RoomNotKickableException extends GameDomainException {

    public RoomNotKickableException(String roomCode) {
        super("ROOM_NOT_KICKABLE", "Room " + roomCode + " is not in a state that allows kicking players",
                Map.of("roomCode", roomCode));
    }
}
