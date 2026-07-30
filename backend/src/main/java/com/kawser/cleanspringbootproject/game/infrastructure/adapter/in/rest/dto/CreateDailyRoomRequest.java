package com.kawser.cleanspringbootproject.game.infrastructure.adapter.in.rest.dto;

import com.kawser.cleanspringbootproject.game.domain.model.GameLanguage;
import jakarta.validation.constraints.NotNull;

/**
 * A daily challenge is solo and anonymous, so unlike CreateRoomRequest there
 * is no hostName to supply — GameApplicationService.createDailyRoom always
 * names the sole player "#".
 */
public record CreateDailyRoomRequest(@NotNull(message = "language is required") GameLanguage language) {
}
