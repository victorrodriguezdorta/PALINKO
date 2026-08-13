package com.kawser.cleanspringbootproject.secretmode.infrastructure.adapter.out.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;

/**
 * Judges secret-mode submissions the same way {@link
 * GroqSecretQualityChecker} does, but against OpenRouter's OpenAI-compatible
 * chat-completions endpoint. Exists purely as {@link
 * FallbackSecretQualityChecker}'s backup, reusing the same
 * model/endpoint/api-key property as
 * game.infrastructure.adapter.out.ai.OpenRouterWordRelationChecker.
 */
@Component
public class OpenRouterSecretQualityChecker extends OpenAiCompatibleSecretQualityChecker {

    private static final Logger log = LoggerFactory.getLogger(OpenRouterSecretQualityChecker.class);
    private static final URI OPENROUTER_ENDPOINT = URI.create("https://openrouter.ai/api/v1/chat/completions");
    private static final String MODEL = "sao10k/l3-lunaris-8b";

    public OpenRouterSecretQualityChecker(@Value("${openrouter.api-key:}") String apiKey) {
        super(OPENROUTER_ENDPOINT, MODEL, apiKey);
    }

    @Override
    protected Logger log() {
        return log;
    }

    @Override
    protected String providerName() {
        return "OpenRouter";
    }

    @Override
    protected String apiKeyPropertyName() {
        return "openrouter.api-key";
    }
}
