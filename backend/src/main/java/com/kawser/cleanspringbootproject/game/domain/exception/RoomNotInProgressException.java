package com.kawser.cleanspringbootproject.game.domain.exception;

public class RoomNotInProgressException extends GameDomainException {

    public RoomNotInProgressException(String roomCode) {
        super("Room " + roomCode + " is not in progress");
    }
}
