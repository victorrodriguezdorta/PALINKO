package com.kawser.cleanspringbootproject.game.infrastructure.adapter.in.stomp.dto;

public record UpdateSettingsMessage(int totalRounds, int answerTimeSeconds, int voteTimeSeconds) {
}
