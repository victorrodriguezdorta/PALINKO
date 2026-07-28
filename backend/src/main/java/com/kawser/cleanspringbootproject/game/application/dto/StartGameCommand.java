package com.kawser.cleanspringbootproject.game.application.dto;

public record StartGameCommand(String roomCode, String playerId, String reconnectToken) {
}
