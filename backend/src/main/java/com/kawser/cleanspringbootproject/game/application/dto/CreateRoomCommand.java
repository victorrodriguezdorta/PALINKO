package com.kawser.cleanspringbootproject.game.application.dto;

public record CreateRoomCommand(String hostName, int totalRounds, int answerTimeSeconds, int voteTimeSeconds) {
}
