package com.kawser.cleanspringbootproject.game.application.port.out;

/**
 * Result of a {@link WordRelationChecker} judgement: how related two words
 * are (0-100) plus a short natural-language justification in the room's
 * language. The justification is informational only (surfaced to callers
 * such as the debug endpoint); game scoring logic uses {@code percentage}
 * alone.
 */
public record WordRelation(int percentage, String justification) {
}
