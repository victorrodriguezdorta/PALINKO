package com.kawser.cleanspringbootproject.game.application.dto;

import com.kawser.cleanspringbootproject.game.domain.model.GameLanguage;

/**
 * A daily challenge is solo and anonymous — there is nobody else in the
 * room to show a name to, so the player never supplies one (see
 * GameApplicationService.createDailyRoom, which always names the host "#").
 */
public record CreateDailyRoomCommand(GameLanguage language) {
}
