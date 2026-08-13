package com.kawser.cleanspringbootproject.secretmode.domain.model;

import com.kawser.cleanspringbootproject.game.domain.model.GameLanguage;

import java.util.Set;

/**
 * A secret-mode daily challenge's data: the hidden rule (hiddenQuality,
 * never sent to the client until the room is SOLVED/GAVE_UP) plus a curated
 * reference bank of words known to satisfy it (acceptedWords) or not
 * (rejectedWords), used to judge a submission without an AI call whenever
 * possible. acceptedWords/rejectedWords are stored lower-cased so lookups
 * never depend on the submitter's casing.
 */
public record Concept(
        String id,
        GameLanguage language,
        String hiddenQuality,
        Set<String> acceptedWords,
        Set<String> rejectedWords) {

    public Concept {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (language == null) {
            throw new IllegalArgumentException("language must not be null");
        }
        if (hiddenQuality == null || hiddenQuality.isBlank()) {
            throw new IllegalArgumentException("hiddenQuality must not be blank");
        }
        acceptedWords = acceptedWords == null ? Set.of() : Set.copyOf(acceptedWords);
        rejectedWords = rejectedWords == null ? Set.of() : Set.copyOf(rejectedWords);
    }
}
