package com.kawser.cleanspringbootproject.secretmode.infrastructure.adapter.out.ai;

import com.kawser.cleanspringbootproject.game.domain.model.GameLanguage;
import com.kawser.cleanspringbootproject.secretmode.application.port.out.SecretQualityCheckException;
import com.kawser.cleanspringbootproject.secretmode.application.port.out.SecretQualityChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * The {@link SecretQualityChecker} Spring actually injects: tries {@link
 * GroqSecretQualityChecker} first, and if it fails for any reason (missing
 * key, network error, non-200 response — most commonly Groq's free tier
 * 500-requests/day limit) retries the exact same judgement against {@link
 * OpenRouterSecretQualityChecker} instead of surfacing a
 * SECRET_QUALITY_CHECK_FAILED error to the player. Mirrors
 * game.infrastructure.adapter.out.ai.FallbackWordRelationChecker.
 */
@Primary
@Component
public class FallbackSecretQualityChecker implements SecretQualityChecker {

    private static final Logger log = LoggerFactory.getLogger(FallbackSecretQualityChecker.class);

    private final GroqSecretQualityChecker groqChecker;
    private final OpenRouterSecretQualityChecker openRouterChecker;

    public FallbackSecretQualityChecker(
            GroqSecretQualityChecker groqChecker, OpenRouterSecretQualityChecker openRouterChecker) {
        this.groqChecker = groqChecker;
        this.openRouterChecker = openRouterChecker;
    }

    @Override
    public WordJudgementResult judgeWord(String word, String hiddenQuality, GameLanguage language) {
        try {
            return groqChecker.judgeWord(word, hiddenQuality, language);
        } catch (SecretQualityCheckException e) {
            log.warn("Groq failed judging word '{}', falling back to OpenRouter", word, e);
            return openRouterChecker.judgeWord(word, hiddenQuality, language);
        }
    }

    @Override
    public QualityMatchResult judgeFormulation(String formulationText, String hiddenQuality, GameLanguage language) {
        try {
            return groqChecker.judgeFormulation(formulationText, hiddenQuality, language);
        } catch (SecretQualityCheckException e) {
            log.warn("Groq failed judging formulation, falling back to OpenRouter", e);
            return openRouterChecker.judgeFormulation(formulationText, hiddenQuality, language);
        }
    }
}
