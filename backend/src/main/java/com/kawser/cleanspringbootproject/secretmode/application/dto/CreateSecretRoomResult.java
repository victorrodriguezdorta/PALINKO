package com.kawser.cleanspringbootproject.secretmode.application.dto;

public record CreateSecretRoomResult(String roomCode, String playerId, String reconnectToken, SecretRoomSnapshot snapshot) {
}
