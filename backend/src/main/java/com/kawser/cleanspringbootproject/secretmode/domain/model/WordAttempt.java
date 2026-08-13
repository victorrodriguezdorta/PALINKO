package com.kawser.cleanspringbootproject.secretmode.domain.model;

import java.time.Instant;

/**
 * One judged word submission — either typed by the player and judged
 * against the word bank/AI, or revealed to them via a hint (source=HINT),
 * which is still recorded as an ordinary ACCEPTED attempt so it shows up
 * mixed into the player's accepted-words list, just visually tagged by
 * source.
 */
public record WordAttempt(
        String id,
        String text,
        WordAttemptOutcome outcome,
        JudgementSource source,
        Instant submittedAt) {
}
