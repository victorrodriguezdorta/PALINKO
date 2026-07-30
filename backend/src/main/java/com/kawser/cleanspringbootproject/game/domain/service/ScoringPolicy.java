package com.kawser.cleanspringbootproject.game.domain.service;

import com.kawser.cleanspringbootproject.game.domain.model.ChainResult;
import com.kawser.cleanspringbootproject.game.domain.model.Player;
import com.kawser.cleanspringbootproject.game.domain.model.Round;

import java.util.Collection;

/**
 * The tunable half of the ruleset, isolated from Round's phase state
 * machine. Two moments call into this: every submitted word (a pure,
 * stateless point calculation) and the accusation vote once a chain reaches
 * REVEAL (which also applies the resulting deltas to Player.score, same as
 * the previous game mode's end-of-round scoring did).
 */
public interface ScoringPolicy {

    /**
     * The minimum AI relatedness percentage (0-100) for a word to count as
     * related, whether that's against the previous chain word or a
     * player's target word.
     */
    int relatednessThreshold();

    /**
     * Points earned for a single submitted word. A rejected word never
     * costs points, it simply earns none. An accepted word earns exactly
     * its AI relatedness percentage against the previous chain word (0-100)
     * as points, plus a flat bonus if it also cleared the relatedness
     * threshold against the author's own target word.
     */
    int scoreWordAttempt(boolean accepted, int relatednessToPrevious, boolean metTargetBonus);

    ChainResult scoreAccusation(Round round, Collection<Player> players);
}
