package com.kawser.cleanspringbootproject.game.application.dto;

import com.kawser.cleanspringbootproject.game.domain.model.GameLanguage;

public record CreateRoomCommand(String hostName, String avatarSeed, GameLanguage language) {
}
