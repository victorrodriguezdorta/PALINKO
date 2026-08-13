package com.kawser.cleanspringbootproject.secretmode.infrastructure.adapter.out.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;

/**
 * Judges secret-mode word/formulation submissions with a Groq-hosted LLM.
 * Reuses the same model/endpoint/api-key property as
 * game.infrastructure.adapter.out.ai.GroqWordRelationChecker so no new
 * credentials are needed - the two adapters just send different prompts to
 * the same provider.
 *
 * <p>Not wired directly as the {@link
 * com.kawser.cleanspringbootproject.secretmode.application.port.out.SecretQualityChecker}
 * bean Spring injects — {@link FallbackSecretQualityChecker} is
 * {@code @Primary} and calls this adapter first, falling back to {@link
 * OpenRouterSecretQualityChecker} if Groq's daily/token rate limit is hit.
 */
@Component
public class GroqSecretQualityChecker extends OpenAiCompatibleSecretQualityChecker {

    private static final Logger log = LoggerFactory.getLogger(GroqSecretQualityChecker.class);
    private static final URI GROQ_ENDPOINT = URI.create("https://api.groq.com/openai/v1/chat/completions");
    private static final String MODEL = "llama-3.3-70b-versatile";

    public GroqSecretQualityChecker(@Value("${groq.api-key:}") String apiKey) {
        super(GROQ_ENDPOINT, MODEL, apiKey);
    }

    @Override
    protected Logger log() {
        return log;
    }

    @Override
    protected String providerName() {
        return "Groq";
    }

    @Override
    protected String apiKeyPropertyName() {
        return "groq.api-key";
    }
}
