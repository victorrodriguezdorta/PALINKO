package com.kawser.cleanspringbootproject.game.application.dto;

import com.kawser.cleanspringbootproject.game.domain.model.RoundPhase;

/**
 * Fired by the PhaseScheduler adapter when a phase's time limit elapses.
 * expectedPhaseIndex/expectedTurnsPlayed/expectedPhase let the use case
 * detect a stale timer (the round already moved on some other way —
 * another word was submitted, the phase already changed, or a later
 * multi-phase chain phase already began) and treat it as a no-op.
 */
public record AdvancePhaseCommand(String roomCode, int expectedPhaseIndex, int expectedTurnsPlayed, RoundPhase expectedPhase) {
}
