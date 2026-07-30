package com.kawser.cleanspringbootproject.game.application.dto;

public record SubmitWordCommand(String roomCode, String playerId, String reconnectToken, String wordText) {
}
