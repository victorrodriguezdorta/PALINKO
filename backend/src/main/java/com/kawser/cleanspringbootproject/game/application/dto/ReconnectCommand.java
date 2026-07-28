package com.kawser.cleanspringbootproject.game.application.dto;

public record ReconnectCommand(String roomCode, String playerId, String reconnectToken) {
}
