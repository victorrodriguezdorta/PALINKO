package com.kawser.cleanspringbootproject.game.domain.exception;

public class WordComparisonFailedException extends GameDomainException {

    public WordComparisonFailedException() {
        super("WORD_COMPARISON_FAILED", "Failed to check word relatedness");
    }
}
