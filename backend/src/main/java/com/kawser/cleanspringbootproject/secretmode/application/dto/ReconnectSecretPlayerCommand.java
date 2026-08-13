package com.kawser.cleanspringbootproject.secretmode.application.dto;

public record ReconnectSecretPlayerCommand(String roomCode, String playerId, String reconnectToken) {
}
