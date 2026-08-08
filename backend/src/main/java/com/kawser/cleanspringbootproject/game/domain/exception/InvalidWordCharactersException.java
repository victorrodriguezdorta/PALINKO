package com.kawser.cleanspringbootproject.game.domain.exception;

import java.util.Map;

/**
 * Raised when a submitted word contains anything other than letters (any
 * script, including accented/tilded ones) before it is ever sent to the AI
 * relatedness judge. Rejected here rather than left to the AI because
 * symbols/digits/spaces/hyphens are exactly what a cheating player would use
 * to smuggle a second word into one submission (e.g. "uva-coche" or
 * "uva coche" to piggyback off "coche") — cheaper and more reliable to
 * refuse the shape outright than to hope the judge catches every disguise.
 */
public class InvalidWordCharactersException extends GameDomainException {

    public InvalidWordCharactersException(String word) {
        super("WORD_INVALID_CHARACTERS", "Word contains invalid characters: '" + word + "'", Map.of("word", word));
    }
}
