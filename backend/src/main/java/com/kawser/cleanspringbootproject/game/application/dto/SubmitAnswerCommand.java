package com.kawser.cleanspringbootproject.game.application.dto;

public record SubmitAnswerCommand(String roomCode, String playerId, String reconnectToken, String answerText) {
}
