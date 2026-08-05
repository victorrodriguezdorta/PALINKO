package com.kawser.cleanspringbootproject.game.infrastructure.adapter.out.ai;

import com.kawser.cleanspringbootproject.game.application.port.out.WordRelationCheckException;
import com.kawser.cleanspringbootproject.game.application.port.out.WordRelationChecker;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Judges word relatedness by asking a Groq-hosted LLM for a 0-100 score and
 * a one-sentence justification, instead of the geometric approximation an
 * embedding cosine gives. This catches relations an embedding model misses
 * (e.g. abstract/associative links, or near-synonyms that embeddings under-
 * score) because the LLM can actually reason about the pair rather than
 * just compare vector geometry.
 *
 * <p>Uses Groq's {@code llama-3.3-70b-versatile} chat model with a terse
 * prompt and a small output budget. The 70B model reasons about word
 * relatedness noticeably better than the 8B instant model, at some cost
 * to latency; that trade favors correctness here since a live game turn
 * on this call.
 * The prompt asks for strict JSON so the response can be parsed without
 * a second round-trip; on any failure (missing API key, network error,
 * non-200 response, malformed JSON) this adapter throws
 * {@link WordRelationCheckException} rather than silently scoring the
 * attempt as 0% related, so callers can tell "genuinely unrelated" apart
 * from "couldn't judge this at all" and surface the latter as an error
 * instead of unfairly rejecting every word.
 *
 * <p>Not wired directly as the {@link WordRelationChecker} bean Spring
 * injects — {@link FallbackWordRelationChecker} is {@code @Primary} and
 * calls this adapter first, falling back to
 * {@link OpenRouterWordRelationChecker} if Groq's daily/token rate limit
 * is hit.
 */
@Component
public class GroqWordRelationChecker extends OpenAiCompatibleWordRelationChecker {

    private static final Logger log = LoggerFactory.getLogger(GroqWordRelationChecker.class);
    private static final URI GROQ_ENDPOINT = URI.create("https://api.groq.com/openai/v1/chat/completions");
    private static final String MODEL = "llama-3.3-70b-versatile";

    public GroqWordRelationChecker(@Value("${groq.api-key:}") String apiKey) {
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
