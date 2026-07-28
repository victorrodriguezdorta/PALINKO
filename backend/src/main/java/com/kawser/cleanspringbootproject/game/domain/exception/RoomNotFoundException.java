package com.kawser.cleanspringbootproject.game.domain.exception;

public class RoomNotFoundException extends GameDomainException {

    public RoomNotFoundException(String roomCode) {
        super("Room not found: " + roomCode);
    }
}
