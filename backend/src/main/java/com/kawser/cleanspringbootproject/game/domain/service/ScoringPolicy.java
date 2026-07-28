package com.kawser.cleanspringbootproject.game.domain.service;

import com.kawser.cleanspringbootproject.game.domain.model.Player;
import com.kawser.cleanspringbootproject.game.domain.model.Round;
import com.kawser.cleanspringbootproject.game.domain.model.RoundResult;

import java.util.Collection;

/**
 * Computes point deltas when a round reaches REVEAL. Isolated from Round
 * itself because scoring is the part of the ruleset most likely to be
 * tuned/rebalanced without touching the round's phase state machine.
 */
public interface ScoringPolicy {

    RoundResult score(Round round, Collection<Player> players);
}
