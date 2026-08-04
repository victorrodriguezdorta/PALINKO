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
 * lazily on first use and kept for the lifetime of the JVM (mirrors {@code
 * SentenceEmbeddingModel}'s load-once approach) — construction parses the
 * whole dictionary, so it isn't cheap enough to redo per word.
 *
 * <p>If the submitted word is already correctly spelled, or Hunspell has no
 * suggestion for it, it's returned unchanged: this only ever fixes clear
 * typos, it never invents a word the player didn't mean.
 */
@Component
public class HunspellWordSpellingCorrector implements WordSpellingCorrector {

    private static final Logger log = LoggerFactory.getLogger(HunspellWordSpellingCorrector.class);
    private static final String DICTIONARY_RESOURCE_PATH = "dictionaries/";

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
            return bestSuggestion;
        } catch (RuntimeException e) {
            log.warn("Hunspell suggestion lookup failed for '{}' ({}); leaving word uncorrected", token, language, e);
            return token;
        }
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
