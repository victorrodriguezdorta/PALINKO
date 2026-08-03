package com.kawser.cleanspringbootproject.game.application.dto;

public record RewindWordCommand(String roomCode, String playerId, String reconnectToken) {
}
