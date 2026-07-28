package com.kawser.cleanspringbootproject.game.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateRoomRequest(
        @NotBlank(message = "hostName cannot be blank") String hostName,
        @Min(value = 1, message = "totalRounds must be at least 1")
        @Max(value = 50, message = "totalRounds must be at most 50") int totalRounds,
        @Min(value = 5, message = "answerTimeSeconds must be at least 5") int answerTimeSeconds,
        @Min(value = 5, message = "voteTimeSeconds must be at least 5") int voteTimeSeconds) {
}
