package com.kawser.cleanspringbootproject.game.domain.model;

/**
 * A single player's guess at which Answer.id belongs to the AI.
 */
public record Vote(String voterPlayerId, String answerId) {

    public Vote {
        if (voterPlayerId == null || voterPlayerId.isBlank()) {
            throw new IllegalArgumentException("voterPlayerId must not be blank");
        }
        if (answerId == null || answerId.isBlank()) {
            throw new IllegalArgumentException("answerId must not be blank");
        }
    }
}
