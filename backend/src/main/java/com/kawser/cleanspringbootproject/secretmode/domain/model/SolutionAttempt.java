package com.kawser.cleanspringbootproject.secretmode.domain.model;

import java.time.Instant;

/**
 * One attempt at declaring the secret quality in natural language.
 * Deliberately carries no numeric accuracy field: exposing a percentage to
 * the client would let a player play "hot/cold" against the score instead
 * of genuinely guessing the quality — only the categorical matchLevel is
 * ever recorded or shown.
 */
public record SolutionAttempt(
        String id,
        String formulationText,
        SolutionMatchLevel matchLevel,
        Instant submittedAt) {
}
