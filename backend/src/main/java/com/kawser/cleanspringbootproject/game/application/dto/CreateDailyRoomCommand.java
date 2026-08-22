package com.kawser.cleanspringbootproject.game.application.dto;

import com.kawser.cleanspringbootproject.game.domain.model.GameLanguage;

/**
 * A daily challenge is solo — there is nobody else in the room to show a
 * name to, so the player never picks one. authenticatedUsername is null for
 * anonymous visitors (see GameApplicationService.createDailyRoom, which then
 * names the host "#"); when present, it comes from the JWT-authenticated
 * SecurityContext rather than client input, so a logged-in player's own
 * username is trustworthy here.
 */
public record CreateDailyRoomCommand(GameLanguage language, String authenticatedUsername) {
}
