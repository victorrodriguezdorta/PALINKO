package com.kawser.cleanspringbootproject.game.infrastructure.adapter.in.stomp.dto;

import com.kawser.cleanspringbootproject.game.domain.model.GameLanguage;

public record UpdateSettingsMessage(
        int wordTimeSeconds, int voteTimeSeconds, GameLanguage language, int infiltratorCount,
        int phaseCount) {
}
