package com.kawser.cleanspringbootproject.game.domain.model;

/**
 * The prompt answered by every player (and the AI) during a round. When
 * source is PLAYER, authorPlayerId identifies who proposed it; when source
 * is BANK, authorPlayerId is null.
 */
public record Question(String id, String text, QuestionSource source, String authorPlayerId) {

    public Question {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text must not be blank");
        }
        if (source == QuestionSource.PLAYER && (authorPlayerId == null || authorPlayerId.isBlank())) {
            throw new IllegalArgumentException("authorPlayerId is required when source is PLAYER");
        }
        if (source == QuestionSource.BANK && authorPlayerId != null) {
            throw new IllegalArgumentException("authorPlayerId must be null when source is BANK");
        }
    }

    public static Question fromBank(String id, String text) {
        return new Question(id, text, QuestionSource.BANK, null);
    }
}
