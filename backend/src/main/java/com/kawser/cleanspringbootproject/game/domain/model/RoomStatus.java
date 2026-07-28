package com.kawser.cleanspringbootproject.game.domain.model;

public enum RoomStatus {
    LOBBY,
    IN_PROGRESS,
    FINISHED,
    /** The host disconnected; the room is torn down and every client is sent home. */
    CLOSED
}
