package com.kawser.cleanspringbootproject.secretmode.application.dto;

import com.kawser.cleanspringbootproject.game.domain.model.GameLanguage;

public record CreateSecretDailyRoomCommand(GameLanguage language) {
}
