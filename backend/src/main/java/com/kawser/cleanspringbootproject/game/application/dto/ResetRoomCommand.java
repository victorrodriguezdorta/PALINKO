package com.kawser.cleanspringbootproject.game.application.dto;

public record ResetRoomCommand(String roomCode, String playerId, String reconnectToken) {
}
