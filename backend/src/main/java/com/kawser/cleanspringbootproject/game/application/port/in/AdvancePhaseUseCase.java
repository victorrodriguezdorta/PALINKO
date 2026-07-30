package com.kawser.cleanspringbootproject.game.application.port.in;

import com.kawser.cleanspringbootproject.game.application.dto.AdvancePhaseCommand;

/**
 * Invoked by the PhaseScheduler adapter when a round's WORD_CHAIN turn or
 * VOTING deadline elapses. Not called by any client-facing adapter
 * directly.
 */
public interface AdvancePhaseUseCase {

    void forceAdvance(AdvancePhaseCommand command);
}
