package com.kawser.cleanspringbootproject.game.application.dto;

public record UpdateRoomSettingsCommand(
        String roomCode,
        String playerId,
        String reconnectToken,
        int totalRounds,
        int answerTimeSeconds,
        int voteTimeSeconds) {
}
