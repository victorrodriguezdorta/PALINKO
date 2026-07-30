package com.kawser.cleanspringbootproject.game.application.dto;

public record SubmitVoteCommand(String roomCode, String playerId, String reconnectToken, String suspectPlayerId) {
}
