package com.kawser.cleanspringbootproject.game.application.port.in;

import com.kawser.cleanspringbootproject.game.application.dto.UpdateRoomSettingsCommand;

/**
 * Host-only, LOBBY-only: lets the host change round count/timers before
 * starting, mirroring StartGameUseCase's host-gating.
 */
public interface UpdateRoomSettingsUseCase {

    void updateRoomSettings(UpdateRoomSettingsCommand command);
}
