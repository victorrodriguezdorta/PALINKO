package com.kawser.cleanspringbootproject.secretmode.application.dto;

public record SubmitSolutionCommand(String roomCode, String playerId, String reconnectToken, String formulationText) {
}
