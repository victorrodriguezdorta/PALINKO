package com.kawser.cleanspringbootproject.secretmode.application.dto;

public record SubmitSecretWordCommand(String roomCode, String playerId, String reconnectToken, String wordText) {
}
