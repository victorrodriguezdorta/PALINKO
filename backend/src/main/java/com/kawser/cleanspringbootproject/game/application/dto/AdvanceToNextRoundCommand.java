package com.kawser.cleanspringbootproject.game.application.dto;

public record AdvanceToNextRoundCommand(String roomCode, String playerId, String reconnectToken) {
}
