package com.kawser.cleanspringbootproject.game.infrastructure.adapter.out.ai;

import com.kawser.cleanspringbootproject.game.application.port.out.WordRelation;
import com.kawser.cleanspringbootproject.game.application.port.out.WordRelationCheckException;
import com.kawser.cleanspringbootproject.game.application.port.out.WordRelationChecker;
import com.kawser.cleanspringbootproject.game.domain.model.GameLanguage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * The {@link WordRelationChecker} Spring actually injects: tries
 * {@link GroqWordRelationChecker} first, and if it fails for any reason
 * (missing key, network error, non-200 response — most commonly Groq's free
 * tier 500-requests/day limit) retries the exact same judgement against
 * {@link OpenRouterWordRelationChecker} instead of surfacing a
 * WORD_COMPARISON_FAILED error to the player.
 */
@Primary
@Component
public class FallbackWordRelationChecker implements WordRelationChecker {

    private static final Logger log = LoggerFactory.getLogger(FallbackWordRelationChecker.class);

    private final GroqWordRelationChecker groqChecker;
    private final OpenRouterWordRelationChecker openRouterChecker;

    public FallbackWordRelationChecker(
            GroqWordRelationChecker groqChecker, OpenRouterWordRelationChecker openRouterChecker) {
        this.groqChecker = groqChecker;
        this.openRouterChecker = openRouterChecker;
    }

    @Override
    public WordRelation relatedness(String wordA, String wordB, GameLanguage language) {
        try {
            return groqChecker.relatedness(wordA, wordB, language);
        } catch (WordRelationCheckException e) {
            log.warn("Groq failed for '{}' / '{}', falling back to OpenRouter", wordA, wordB, e);
            return openRouterChecker.relatedness(wordA, wordB, language);
        }
    }
}
