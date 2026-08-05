package com.kawser.cleanspringbootproject.game.infrastructure.adapter.out.ai;

import com.kawser.cleanspringbootproject.game.application.port.out.WordRelationCheckException;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Judges word relatedness the same way {@link GroqWordRelationChecker} does,
 * but against OpenRouter's OpenAI-compatible chat-completions endpoint.
 * Exists purely as {@link FallbackWordRelationChecker}'s backup: Groq's free
 * tier caps out at 500 requests/day, which a handful of concurrent games can
 * exhaust, so once Groq starts throwing {@link WordRelationCheckException}
 * (rate limit or any other failure) this adapter takes over with the same
 * system/user prompt and the same 0-100 + justification response shape.
 */
@Component
public class OpenRouterWordRelationChecker extends OpenAiCompatibleWordRelationChecker {

    private static final Logger log = LoggerFactory.getLogger(OpenRouterWordRelationChecker.class);
    private static final URI OPENROUTER_ENDPOINT = URI.create("https://openrouter.ai/api/v1/chat/completions");
    private static final String MODEL = "sao10k/l3-lunaris-8b";

    public OpenRouterWordRelationChecker(@Value("${openrouter.api-key:}") String apiKey) {
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
