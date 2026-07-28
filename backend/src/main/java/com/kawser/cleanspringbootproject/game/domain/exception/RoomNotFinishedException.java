package com.kawser.cleanspringbootproject.game.domain.exception;

public class RoomNotFinishedException extends GameDomainException {

    public RoomNotFinishedException(String roomCode) {
        super("Room " + roomCode + " has not finished yet");
    }
}
