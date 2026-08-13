package com.kawser.cleanspringbootproject.secretmode.application.dto;

public record GiveUpCommand(String roomCode, String playerId, String reconnectToken) {
}
