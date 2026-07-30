package com.kawser.cleanspringbootproject.game.domain.model;

/**
 * The three words dealt out at the start of a chain: everyone starts from
 * startWord, every crew member's target is groupTargetWord, and the
 * infiltrator's target is infiltratorTargetWord instead — a different word
 * they are never told differs from everyone else's.
 */
public record WordSet(String startWord, String groupTargetWord, String infiltratorTargetWord) {

    public WordSet {
        if (startWord == null || startWord.isBlank()) {
            throw new IllegalArgumentException("startWord must not be blank");
        }
        if (groupTargetWord == null || groupTargetWord.isBlank()) {
            throw new IllegalArgumentException("groupTargetWord must not be blank");
        }
        if (infiltratorTargetWord == null || infiltratorTargetWord.isBlank()) {
            throw new IllegalArgumentException("infiltratorTargetWord must not be blank");
        }
        if (groupTargetWord.trim().equalsIgnoreCase(infiltratorTargetWord.trim())) {
            throw new IllegalArgumentException("groupTargetWord and infiltratorTargetWord must differ");
        }
    }
}
