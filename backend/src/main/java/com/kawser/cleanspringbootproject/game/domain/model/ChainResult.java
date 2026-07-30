package com.kawser.cleanspringbootproject.game.domain.model;

import java.util.Map;
import java.util.Set;

/**
 * Outcome of a finished chain: who the infiltrator(s) actually were (empty
 * for a cooperative, no-infiltrator room), who (if anyone) the majority
 * accused, whether the crew came out on top, and the per-player point
 * deltas awarded for this outcome, keyed by playerId.
 *
 * <p>{@code endedByInfiltratorWord} marks the instant-loss ending (see
 * {@link Round#loseToInfiltrator()}): a non-infiltrator wrote the
 * infiltrators' secret target word, ending the game immediately without a
 * vote ever happening. It is always false for both the normal
 * accusation-vote ending and the no-infiltrator cooperative ending.
 */
public record ChainResult(
        Set<String> infiltratorPlayerIds,
        String infiltratorTargetWord,
        String accusedPlayerId,
        boolean crewWon,
        Map<String, Integer> scoreDeltaByPlayerId,
        boolean endedByInfiltratorWord) {

    public ChainResult {
        if (infiltratorPlayerIds == null) {
            throw new IllegalArgumentException("infiltratorPlayerIds must not be null");
        }
        infiltratorPlayerIds = Set.copyOf(infiltratorPlayerIds);
        if (!infiltratorPlayerIds.isEmpty() && (infiltratorTargetWord == null || infiltratorTargetWord.isBlank())) {
            throw new IllegalArgumentException("infiltratorTargetWord must not be blank when there are infiltrators");
        }
        scoreDeltaByPlayerId = Map.copyOf(scoreDeltaByPlayerId);
    }

    public int deltaFor(String playerId) {
        return scoreDeltaByPlayerId.getOrDefault(playerId, 0);
    }
}
