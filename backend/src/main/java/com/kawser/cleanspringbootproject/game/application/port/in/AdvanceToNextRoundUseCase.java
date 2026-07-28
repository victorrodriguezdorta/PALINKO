package com.kawser.cleanspringbootproject.game.application.port.in;

import com.kawser.cleanspringbootproject.game.application.dto.AdvanceToNextRoundCommand;

/**
 * Triggered only by an explicit host action (there is no timer for
 * REVEAL -> next round), unlike ANSWERING/VOTING which always advance on
 * their own.
 */
public interface AdvanceToNextRoundUseCase {

    void advanceToNextRound(AdvanceToNextRoundCommand command);
}
