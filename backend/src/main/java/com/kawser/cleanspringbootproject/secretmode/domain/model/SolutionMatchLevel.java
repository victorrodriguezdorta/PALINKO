package com.kawser.cleanspringbootproject.secretmode.domain.model;

/**
 * How closely a player's declared formulation matches the real secret
 * quality, as classified directly by the AI judge's prompt (never derived
 * from an exposed numeric score — see SecretQualityChecker.judgeFormulation
 * javadoc for why no percentage is ever surfaced to the client). Only
 * CORRECT ends the game.
 */
public enum SolutionMatchLevel {
    CORRECT,
    PARTIAL,
    INCORRECT
}
