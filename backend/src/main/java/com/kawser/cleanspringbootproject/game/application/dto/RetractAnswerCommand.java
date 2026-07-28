package com.kawser.cleanspringbootproject.game.application.dto;

public record RetractAnswerCommand(String roomCode, String playerId, String reconnectToken) {
}
