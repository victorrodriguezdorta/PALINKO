package com.kawser.cleanspringbootproject.game.application.port.out;

import com.kawser.cleanspringbootproject.game.domain.model.GameLanguage;

/**
 * Judges how semantically related two words are, as a 0-100 percentage plus
 * a short justification. The relatedness judgement is language-specific (a
 * real implementation would need different models/dictionaries per
 * language), so every room's own language is passed through here rather
 * than assumed.
 */
public interface WordRelationChecker {

    WordRelation relatedness(String wordA, String wordB, GameLanguage language);
}
