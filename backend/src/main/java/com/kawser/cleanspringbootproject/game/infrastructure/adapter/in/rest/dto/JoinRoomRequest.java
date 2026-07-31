package com.kawser.cleanspringbootproject.game.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinRoomRequest(
        @NotBlank(message = "playerName cannot be blank")
        @Size(max = 24, message = "playerName must be at most 24 characters") String playerName,
        @NotBlank(message = "avatarSeed cannot be blank") String avatarSeed) {
}
