package com.kawser.cleanspringbootproject.game.domain.model;

/**
 * Per-room configuration chosen by the host when creating the room.
 */
public record RoomSettings(int totalRounds, int answerTimeSeconds, int voteTimeSeconds) {

    public RoomSettings {
        if (totalRounds < 1) {
            throw new IllegalArgumentException("totalRounds must be at least 1");
        }
        if (answerTimeSeconds < 1) {
            throw new IllegalArgumentException("answerTimeSeconds must be at least 1");
        }
        if (voteTimeSeconds < 1) {
            throw new IllegalArgumentException("voteTimeSeconds must be at least 1");
        }
    }
}
