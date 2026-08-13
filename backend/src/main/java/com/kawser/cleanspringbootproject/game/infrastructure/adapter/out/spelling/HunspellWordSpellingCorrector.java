package com.kawser.cleanspringbootproject.game.infrastructure.adapter.out.spelling;

import com.kawser.cleanspringbootproject.game.application.port.out.WordSpellingCorrector;
import com.kawser.cleanspringbootproject.game.domain.model.GameLanguage;
import dumonts.hunspell.Hunspell;
import jakarta.annotation.PreDestroy;
import java.util.EnumMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Corrects spelling with Hunspell (the same spell-checking engine used by
 * LibreOffice/Firefox), running fully offline against the dictionaries
 * bundled under {@code src/main/resources/dictionaries}. Chosen over an
 * LLM round-trip because it's a local, near-instant lookup rather than a
 * network call on every submitted word, and over a full grammar engine
 * (e.g. LanguageTool) because loading one Hunspell instance per language is
 * a few MB, not hundreds.
 *
 * <p>One {@link Hunspell} instance per {@link GameLanguage} is created
 * lazily on first use and kept for the lifetime of the JVM — construction
 * parses the whole dictionary, so it isn't cheap enough to redo per word.
 *
 * <p>If the submitted word is already correctly spelled, or Hunspell has no
 * suggestion for it, it's returned unchanged: this only ever fixes clear
 * typos, it never invents a word the player didn't mean.
 *
 * <p>Hunspell's top suggestion is only trusted when it is a close edit of
 * the original word (see {@link #isCloseEnough}). Words that Hunspell's
 * dictionary simply doesn't contain (e.g. valid but uncommon nouns like
 * "koala") would otherwise get "corrected" to an unrelated dictionary word
 * that merely scores well on Hunspell's phonetic/keyboard-distance ranking
 * (e.g. "toalla") — an edit-distance gate keeps corrections to genuine
 * typos of an intended word instead.
 */
@Component
public class HunspellWordSpellingCorrector implements WordSpellingCorrector {

    private static final Logger log = LoggerFactory.getLogger(HunspellWordSpellingCorrector.class);
    private static final String DICTIONARY_RESOURCE_PATH = "dictionaries/";

    /**
     * Maximum Levenshtein distance, as a fraction of the original word's
     * length, for a Hunspell suggestion to be trusted. 1 letter can be
     * wrong/missing/extra in a 4-letter word, but a 5-6 letter word needing
     * 3+ edits is almost certainly a different word entirely, not a typo.
     */
    private static final double MAX_EDIT_DISTANCE_RATIO = 0.34;

    private final Map<GameLanguage, Hunspell> instances = new EnumMap<>(GameLanguage.class);
    private final Object loadLock = new Object();

    @Override
    public String correct(String word, GameLanguage language) {
        String trimmed = word.trim();
        if (trimmed.isEmpty()) {
            return word;
        }

        // Players may submit up to two words (e.g. "buenos dias"); each
        // token is spell-checked independently so a typo in one doesn't
        // send the whole phrase to Hunspell as a single unknown string,
        // and a correct word next to a typo isn't needlessly touched.
        String[] tokens = trimmed.split("\\s+");
        if (tokens.length == 1) {
            return correctSingleWord(tokens[0], language);
        }

        StringBuilder corrected = new StringBuilder(trimmed.length());
        for (int i = 0; i < tokens.length; i++) {
            if (i > 0) {
                corrected.append(' ');
            }
            corrected.append(correctSingleWord(tokens[i], language));
        }
        return corrected.toString();
    }

    private String correctSingleWord(String token, GameLanguage language) {
        Hunspell hunspell = instanceFor(language);
        if (hunspell == null || hunspell.spell(token)) {
            return token;
        }

        try {
            String[] suggestions = hunspell.suggest(token);
            if (suggestions.length == 0) {
                return token;
            }
            // Hunspell ranks suggestions best-first; only the top one is
            // used, and only when it's a single word - a multi-word
            // suggestion (e.g. splitting "porfavor" into "por favor") isn't
            // a same-word correction, so it's skipped in favor of leaving
            // the original typo for the relatedness judge to score as-is.
            String bestSuggestion = suggestions[0];
            if (bestSuggestion.indexOf(' ') >= 0) {
                return token;
            }
            if (!isCloseEnough(token, bestSuggestion)) {
                return token;
            }
            return bestSuggestion;
        } catch (RuntimeException e) {
            log.warn("Hunspell suggestion lookup failed for '{}' ({}); leaving word uncorrected", token, language, e);
            return token;
        }
    }

    /**
     * True when {@code suggestion} is within {@link #MAX_EDIT_DISTANCE_RATIO}
     * of {@code original}'s length in Levenshtein distance, and at least 1
     * edit is allowed even for very short words.
     */
    private boolean isCloseEnough(String original, String suggestion) {
        int maxDistance = Math.max(1, (int) Math.floor(original.length() * MAX_EDIT_DISTANCE_RATIO));
        return levenshteinDistance(original.toLowerCase(), suggestion.toLowerCase(), maxDistance) <= maxDistance;
    }

    /**
     * Levenshtein distance capped at {@code maxDistance}: once every entry
     * in the current row exceeds the cap, the words are already too far
     * apart, so the row-scan bails out early rather than filling the whole
     * (potentially large) DP table for a suggestion that will be rejected
     * anyway.
     */
    private int levenshteinDistance(String a, String b, int maxDistance) {
        int m = a.length();
        int n = b.length();
        if (Math.abs(m - n) > maxDistance) {
            return maxDistance + 1;
        }

        int[] previousRow = new int[n + 1];
        int[] currentRow = new int[n + 1];
        for (int j = 0; j <= n; j++) {
            previousRow[j] = j;
        }

        for (int i = 1; i <= m; i++) {
            currentRow[0] = i;
            int rowMin = currentRow[0];
            for (int j = 1; j <= n; j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                currentRow[j] = Math.min(
                        Math.min(currentRow[j - 1] + 1, previousRow[j] + 1),
                        previousRow[j - 1] + cost);
                rowMin = Math.min(rowMin, currentRow[j]);
            }
            if (rowMin > maxDistance) {
                return maxDistance + 1;
            }
            int[] swap = previousRow;
            previousRow = currentRow;
            currentRow = swap;
        }
        return previousRow[n];
    }

    private Hunspell instanceFor(GameLanguage language) {
        Hunspell existing = instances.get(language);
        if (existing != null) {
            return existing;
        }
        synchronized (loadLock) {
            return instances.computeIfAbsent(language, this::load);
        }
    }

    private Hunspell load(GameLanguage language) {
        String dictionaryCode = switch (language) {
            case SPANISH -> "es_ES";
            case ENGLISH -> "en_US";
        };
        try {
            return Hunspell.forDictionaryInResources(dictionaryCode, DICTIONARY_RESOURCE_PATH);
        } catch (RuntimeException e) {
            log.warn("Could not load Hunspell dictionary '{}'; spelling correction disabled for {}",
                    dictionaryCode, language, e);
            return null;
        }
    }

    @PreDestroy
    void closeAll() {
        instances.values().forEach(hunspell -> {
            if (hunspell != null) {
                hunspell.close();
            }
        });
    }
}
