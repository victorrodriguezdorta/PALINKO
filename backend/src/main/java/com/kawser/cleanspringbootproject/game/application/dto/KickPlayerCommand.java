package com.kawser.cleanspringbootproject.game.application.dto;

public record KickPlayerCommand(String roomCode, String playerId, String reconnectToken, String targetPlayerId) {
}
