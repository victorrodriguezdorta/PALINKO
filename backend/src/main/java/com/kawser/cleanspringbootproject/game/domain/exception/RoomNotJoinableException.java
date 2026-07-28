package com.kawser.cleanspringbootproject.game.domain.exception;

public class RoomNotJoinableException extends GameDomainException {

    public RoomNotJoinableException(String roomCode) {
        super("Room " + roomCode + " is not accepting new players");
    }
}
