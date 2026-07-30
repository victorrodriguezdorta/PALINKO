package com.kawser.cleanspringbootproject.game.application.port.out;

import com.kawser.cleanspringbootproject.game.domain.model.GameLanguage;
import com.kawser.cleanspringbootproject.game.domain.model.WordSet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Supplies the words dealt out for each phase of a chain, in the room's own
 * language. Target words are chosen to be semantically far from the phase's
 * start word (low relatedness), so the chain is deliberately hard to
 * bridge.
 */
public interface ChainWordBank {

    /**
     * Deals the very first phase: picks its own start word plus a
     * low-similarity group target and a separate low-similarity infiltrator
     * target. Nothing to exclude yet since no phase has been dealt.
     */
    WordSet firstWordSet(GameLanguage language);

    /**
     * Deals a subsequent phase: startWord is fixed (the word the previous
     * phase's chain ended on), and both new target words are freshly
     * picked, excluding usedWords (every start/target word already dealt
     * earlier this game, including startWord itself) so a multi-phase game
     * never revisits the same word.
     */
    WordSet nextPhaseWordSet(GameLanguage language, String startWord, Set<String> usedWords);

    /**
     * Deals every phase of a game up front: phase 1 comes from {@link
     * #firstWordSet}, and each subsequent phase's start word is the
     * previous phase's own group target word (the word the crew is
     * meant to land on to complete that phase) rather than whatever word
     * actually gets played — so the whole chain, including phases not yet
     * reached, is known and can be shown to players from the very start of
     * the game.
     */
    default List<WordSet> fullChain(GameLanguage language, int phaseCount) {
        if (phaseCount < 1) {
            throw new IllegalArgumentException("phaseCount must be at least 1");
        }
        List<WordSet> chain = new ArrayList<>(phaseCount);
        Set<String> usedWords = new HashSet<>();
        WordSet wordSet = firstWordSet(language);
        chain.add(wordSet);
        usedWords.add(wordSet.startWord());
        usedWords.add(wordSet.groupTargetWord());
        usedWords.add(wordSet.infiltratorTargetWord());
        for (int i = 1; i < phaseCount; i++) {
            wordSet = nextPhaseWordSet(language, wordSet.groupTargetWord(), usedWords);
            chain.add(wordSet);
            usedWords.add(wordSet.groupTargetWord());
            usedWords.add(wordSet.infiltratorTargetWord());
        }
        return List.copyOf(chain);
    }

    /**
     * Deals every phase of a game exactly like {@link #fullChain}, but
     * fully deterministically for a given seed: every call with the same
     * seed, language and phaseCount returns the identical chain, letting
     * callers (namely the daily-challenge mode, seeded from the current
     * UTC date) hand out the same words to every player without any of
     * them replaying a live game's own randomness. Adapters that back
     * their word selection with a {@link java.util.Random} should override
     * this to substitute a fresh, seeded Random for the one call rather
     * than mutating their own shared instance (which live multiplayer
     * rooms keep drawing from concurrently).
     */
    default List<WordSet> fullChain(GameLanguage language, int phaseCount, long seed) {
        return fullChain(language, phaseCount);
    }
}
