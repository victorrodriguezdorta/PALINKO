package com.kawser.cleanspringbootproject.game.domain.model;

/**
 * One submitted answer for a round's question. The id is the anonymous
 * handle shown to voters — authorPlayerId must stay out of any payload sent
 * to clients before the round reaches REVEAL.
 * isAi is explicit (rather than inferred from authorPlayerId == null) so a
 * real AI provider can later replace the mock without changing this shape.
 */
public record Answer(String id, String text, String authorPlayerId, boolean isAi) {

    public Answer {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text must not be blank");
        }
        if (isAi && authorPlayerId != null) {
            throw new IllegalArgumentException("authorPlayerId must be null for the AI answer");
        }
        if (!isAi && (authorPlayerId == null || authorPlayerId.isBlank())) {
            throw new IllegalArgumentException("authorPlayerId is required for a human answer");
        }
    }

    public static Answer human(String id, String authorPlayerId, String text) {
        return new Answer(id, text, authorPlayerId, false);
    }

    public static Answer ai(String id, String text) {
        return new Answer(id, text, null, true);
    }
}
