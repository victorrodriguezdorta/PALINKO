package com.kawser.cleanspringbootproject.game.domain.model;

/**
 * A single player's accusation of which player they believe is the
 * infiltrator.
 */
public record Vote(String voterPlayerId, String suspectPlayerId) {

    public Vote {
        if (voterPlayerId == null || voterPlayerId.isBlank()) {
            throw new IllegalArgumentException("voterPlayerId must not be blank");
        }
        if (suspectPlayerId == null || suspectPlayerId.isBlank()) {
            throw new IllegalArgumentException("suspectPlayerId must not be blank");
        }
    }
}
