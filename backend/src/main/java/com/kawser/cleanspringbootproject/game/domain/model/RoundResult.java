package com.kawser.cleanspringbootproject.game.domain.model;

import java.util.List;
import java.util.Map;

/**
 * Outcome of scoring a round once it reaches REVEAL: the AI's real answer id
 * and the per-player point deltas earned this round (correct AI guesses +
 * "deceived someone" bonuses), keyed by playerId.
 */
public record RoundResult(String aiAnswerId, Map<String, Integer> scoreDeltaByPlayerId) {

    public RoundResult {
        if (aiAnswerId == null || aiAnswerId.isBlank()) {
            throw new IllegalArgumentException("aiAnswerId must not be blank");
        }
        scoreDeltaByPlayerId = Map.copyOf(scoreDeltaByPlayerId);
    }

    public int deltaFor(String playerId) {
        return scoreDeltaByPlayerId.getOrDefault(playerId, 0);
    }

    public static RoundResult empty(String aiAnswerId) {
        return new RoundResult(aiAnswerId, Map.of());
    }

    public List<String> scoredPlayerIds() {
        return List.copyOf(scoreDeltaByPlayerId.keySet());
    }
}
