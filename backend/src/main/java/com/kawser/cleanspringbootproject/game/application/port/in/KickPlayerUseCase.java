package com.kawser.cleanspringbootproject.game.application.port.in;

import com.kawser.cleanspringbootproject.game.application.dto.KickPlayerCommand;

/**
 * Host-only, LOBBY-only: lets the host remove another player from the room
 * before starting, mirroring UpdateRoomSettingsUseCase's host-gating.
 */
public interface KickPlayerUseCase {

    void kickPlayer(KickPlayerCommand command);
}
