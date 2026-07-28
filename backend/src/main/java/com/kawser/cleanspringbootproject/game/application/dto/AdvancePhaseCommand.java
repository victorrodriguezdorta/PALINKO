package com.kawser.cleanspringbootproject.game.application.dto;

import com.kawser.cleanspringbootproject.game.domain.model.RoundPhase;

/**
 * Fired by the PhaseScheduler adapter when a phase's time limit elapses.
 * roundNumber/expectedPhase let the use case detect a stale timer (the room
 * already moved on some other way) and treat it as a no-op.
 */
public record AdvancePhaseCommand(String roomCode, int roundNumber, RoundPhase expectedPhase) {
}
