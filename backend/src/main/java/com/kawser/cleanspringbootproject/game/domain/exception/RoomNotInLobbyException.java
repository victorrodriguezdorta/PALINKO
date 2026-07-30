package com.kawser.cleanspringbootproject.game.domain.exception;

import java.util.Map;

public class RoomNotInLobbyException extends GameDomainException {

    public RoomNotInLobbyException(String roomCode) {
        super("ROOM_NOT_IN_LOBBY", "Room " + roomCode + " is not in the lobby", Map.of("roomCode", roomCode));
    }
}
