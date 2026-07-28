package com.kawser.cleanspringbootproject.game.domain.exception;

public class RoomNotInLobbyException extends GameDomainException {

    public RoomNotInLobbyException(String roomCode) {
        super("Room " + roomCode + " is not in the lobby");
    }
}
