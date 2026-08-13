package com.kawser.cleanspringbootproject.secretmode.infrastructure.adapter.in.rest.dto;

import com.kawser.cleanspringbootproject.game.domain.model.GameLanguage;
import jakarta.validation.constraints.NotNull;

public record CreateSecretDailyRoomRequest(@NotNull(message = "language is required") GameLanguage language) {
}
