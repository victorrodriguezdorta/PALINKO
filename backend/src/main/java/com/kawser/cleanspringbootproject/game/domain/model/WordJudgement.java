package com.kawser.cleanspringbootproject.game.domain.model;

/**
 * The already-computed verdict for a submitted word, handed down from the
 * application layer (which owns the WordRelationChecker and ScoringPolicy
 * collaborators) to Round.submitWord so the aggregate itself never has to
 * reach out to either port.
 */
public record WordJudgement(
        boolean accepted,
        int relatednessToPrevious,
        String justification,
        Integer relatednessToTarget,
        boolean reachedTarget) {
}
