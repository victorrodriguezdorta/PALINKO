package com.kawser.cleanspringbootproject.secretmode.application.dto;

public record UseHintCommand(String roomCode, String playerId, String reconnectToken) {
}
