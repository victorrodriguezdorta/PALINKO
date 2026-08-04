package com.kawser.cleanspringbootproject.game.infrastructure.adapter.out.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kawser.cleanspringbootproject.game.application.port.out.WordRelation;
import com.kawser.cleanspringbootproject.game.application.port.out.WordRelationChecker;
import com.kawser.cleanspringbootproject.game.domain.model.GameLanguage;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
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
 * prompt and a small output budget, since round-trip latency directly
 * delays every word submission in a live game. Groq's LPU inference keeps
 * this fast despite the larger model, and the extra reasoning quality over
 * the previous 8B model catches more of the abstract/associative relations
 * this judgment call depends on. The prompt asks for strict JSON so the
 * response can be parsed without a second round-trip; on any failure
 * (network error, malformed JSON, missing key) this adapter fails closed
 * with a 0% relation rather than blocking the game.
 */
@Primary
@Component
public class GroqWordRelationChecker implements WordRelationChecker {

    private static final Logger log = LoggerFactory.getLogger(GroqWordRelationChecker.class);
    private static final URI GROQ_ENDPOINT = URI.create("https://api.groq.com/openai/v1/chat/completions");
    private static final String MODEL = "llama-3.3-70b-versatile";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String apiKey;

    public GroqWordRelationChecker(@Value("${groq.api-key:}") String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public WordRelation relatedness(String wordA, String wordB, GameLanguage language) {
        String a = wordA.trim();
        String b = wordB.trim();
        if (a.equalsIgnoreCase(b)) {
            return new WordRelation(100, null);
        }
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("groq.api-key is not configured; returning 0% relatedness for '{}' / '{}'", a, b);
            return new WordRelation(0, null);
        }

        try {
            String requestBody = objectMapper.writeValueAsString(chatRequest(a, b, language));
            HttpRequest request = HttpRequest.newBuilder(GROQ_ENDPOINT)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Groq API returned status {} for '{}' / '{}': {}", response.statusCode(), a, b, response.body());
                return new WordRelation(0, null);
            }
            return parseRelation(response.body());
        } catch (Exception e) {
            log.warn("Groq API call failed for '{}' / '{}'", a, b, e);
            return new WordRelation(0, null);
        }
    }

    private Object chatRequest(String wordA, String wordB, GameLanguage language) {
        String replyLanguage = language == GameLanguage.SPANISH ? "Spanish" : "English";
        String systemPrompt = "You judge how related two words or short phrases are, for a word-chain party "
                + "game where players connect words the way a human free-associates, not the way a thesaurus "
                + "does. Score generously: give credit not only to synonyms and same-category words, but also to "
                + "cause-and-effect pairs (rain / umbrella), part-whole pairs (wheel / car), typical "
                + "co-occurrence or context (beach / sunscreen), tool-and-user or tool-and-purpose (needle / "
                + "sew), opposites (hot / cold), and well-known cultural, idiomatic, or pop-culture associations "
                + "(Batman / Gotham). A pair only deserves a low score when a reasonable person would find no "
                + "plausible link between them at all, even a loose or indirect one. "
                + "Reply with ONLY compact JSON: {\"percentage\": <0-100 integer>, \"reason\": \"<very short "
                + "justification>\"}. The \"reason\" text must be written in " + replyLanguage
                + ". No markdown, no extra text.";
        String userPrompt = "Word A: \"" + wordA + "\"\nWord B: \"" + wordB + "\"";

        return new ChatRequest(
                MODEL,
                new ChatMessage[] {
                    new ChatMessage("system", systemPrompt),
                    new ChatMessage("user", userPrompt)
                },
                0.0,
                80,
                new ResponseFormat("json_object"));
    }

    private WordRelation parseRelation(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        String content = root.path("choices").path(0).path("message").path("content").asText();
        JsonNode payload = objectMapper.readTree(content);

        int percentage = Math.max(0, Math.min(100, payload.path("percentage").asInt(0)));
        String reason = payload.path("reason").asText(null);
        return new WordRelation(percentage, reason);
    }

    private record ChatRequest(String model, ChatMessage[] messages, double temperature, int max_tokens,
            ResponseFormat response_format) {
    }

    private record ChatMessage(String role, String content) {
    }

    private record ResponseFormat(String type) {
    }
}
