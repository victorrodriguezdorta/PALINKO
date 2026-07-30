package com.kawser.cleanspringbootproject.game.application.port.out;

import com.kawser.cleanspringbootproject.game.domain.model.GameLanguage;

/**
 * Fixes spelling mistakes in a just-submitted word, in the room's own
 * language, before it is ever compared against another word for
 * relatedness. Correcting first means a misspelling like "porfesor" is
 * judged (and stored) as "profesor" instead of confusing the relatedness
 * judge with a word it doesn't recognize. Returns the input unchanged
 * whenever it's already correctly spelled or no confident correction is
 * available.
 */
public interface WordSpellingCorrector {

    String correct(String word, GameLanguage language);
}
