package com.kawser.cleanspringbootproject.game.application.port.in;

import com.kawser.cleanspringbootproject.game.application.dto.ResetRoomCommand;

/**
 * Triggered only by an explicit host action ("play again") once the room
 * has FINISHED, mirroring AdvanceToNextRoundUseCase's host-only, no-timer
 * lifecycle step.
 */
public interface ResetRoomUseCase {

    void resetRoom(ResetRoomCommand command);
}
