package com.kawser.cleanspringbootproject.secretmode.application.port.out;

import com.kawser.cleanspringbootproject.game.domain.model.GameLanguage;
import com.kawser.cleanspringbootproject.secretmode.domain.model.SolutionMatchLevel;

/**
 * Judges secret-mode submissions against an AI, used whenever the curated
 * Concept word bank doesn't already have an answer (see
 * game.application.port.out.WordRelationChecker for the original mode's
 * equivalent role, though the prompts/response shapes here are unrelated to
 * that one — word-chain relatedness and hidden-quality judgement are
 * different questions).
 */
public interface SecretQualityChecker {

    /**
     * Whether a single candidate word genuinely satisfies the secret
     * quality (not merely thematically related to it).
     */
    WordJudgementResult judgeWord(String word, String hiddenQuality, GameLanguage language);

    /**
     * How closely the player's own natural-language guess at the secret
     * quality matches the real one. Deliberately returns only a categorical
     * matchLevel, never a numeric score: exposing a percentage would let a
     * player play "hot/cold" against the number instead of genuinely
     * guessing the quality.
     */
    QualityMatchResult judgeFormulation(String formulationText, String hiddenQuality, GameLanguage language);

    record WordJudgementResult(boolean accepted, String reason) {
    }

    record QualityMatchResult(SolutionMatchLevel matchLevel, String reason) {
    }
}
