package com.kawser.cleanspringbootproject.game.domain.model;

/**
 * The language a room plays in, fixed by whichever language the host chose
 * (see RoomSettings). Both the word bank (ChainWordBank) and the word
 * relation judge (WordRelationChecker) are looked up per-language, so
 * adding a new language here means adding its word list/relatedness logic
 * to those adapters, without touching this enum's callers.
 */
public enum GameLanguage {
    ENGLISH,
    SPANISH
}
