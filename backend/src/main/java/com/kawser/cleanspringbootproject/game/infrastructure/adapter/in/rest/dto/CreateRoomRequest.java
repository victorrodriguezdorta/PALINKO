package com.kawser.cleanspringbootproject.game.infrastructure.adapter.in.rest.dto;

import com.kawser.cleanspringbootproject.game.domain.model.GameLanguage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Room rules (turn/vote timers, max turns) are intentionally not settable
 * here — they can only be changed by the host from inside the LOBBY (see
 * UpdateRoomSettingsUseCase), so a room is always created with
 * RoomSettings.defaults(language) and the create step only has to expose
 * the host's own chosen language.
 */
public record CreateRoomRequest(
        @NotBlank(message = "hostName cannot be blank") String hostName,
        @NotBlank(message = "avatarSeed cannot be blank") String avatarSeed,
        @NotNull(message = "language is required") GameLanguage language) {
}
