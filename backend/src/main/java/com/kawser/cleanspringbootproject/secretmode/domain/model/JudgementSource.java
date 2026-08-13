package com.kawser.cleanspringbootproject.secretmode.domain.model;

/**
 * How a WordAttempt's outcome was determined — shown to the player so they
 * can tell a curated word-bank answer apart from one the AI had to judge
 * live, or one revealed to them as a hint.
 */
public enum JudgementSource {
    WORD_BANK,
    AI,
    HINT
}
