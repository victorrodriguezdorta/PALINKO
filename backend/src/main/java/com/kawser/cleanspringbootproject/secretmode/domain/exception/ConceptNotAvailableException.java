package com.kawser.cleanspringbootproject.secretmode.domain.exception;

/**
 * Operational error: the ConceptBank has no concept configured for the
 * requested day (empty/misconfigured calendar or missing concept file).
 * Not a player-facing rule violation — surfaced as a 500.
 */
public class ConceptNotAvailableException extends SecretGameDomainException {

    public ConceptNotAvailableException(String conceptId) {
        super("CONCEPT_NOT_AVAILABLE", "Secret mode concept not available: " + conceptId);
    }
}
