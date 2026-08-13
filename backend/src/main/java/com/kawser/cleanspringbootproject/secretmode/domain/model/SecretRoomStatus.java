package com.kawser.cleanspringbootproject.secretmode.domain.model;

/**
 * A secret-mode room has no LOBBY: a daily room is created already
 * IN_PROGRESS, exactly like game.domain.model.Room's daily challenge.
 */
public enum SecretRoomStatus {
    IN_PROGRESS,
    SOLVED,
    GAVE_UP,
    CLOSED
}
