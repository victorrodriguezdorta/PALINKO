package com.kawser.cleanspringbootproject.game.application.dto;

import com.kawser.cleanspringbootproject.game.domain.model.GameLanguage;

public record UpdateRoomSettingsCommand(
        String roomCode,
        String playerId,
        String reconnectToken,
        int wordTimeSeconds,
        int voteTimeSeconds,
        GameLanguage language,
        int infiltratorCount,
        int phaseCount) {
}
