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
 * <p>Uses Groq's {@code llama-3.1-8b-instant} chat model with a terse
 * prompt and a small output budget, since round-trip latency directly
 * delays every word submission in a live game. This 8B model trades some
 * reasoning quality for materially lower latency than the 70B model,
 * which matters more here given how often this call is made per game.
 * The prompt asks for strict JSON so the response can be parsed without
 * a second round-trip; on any failure (network error, malformed JSON,
 * missing key) this adapter fails closed with a 0% relation rather than
 * blocking the game.
 */
@Primary
@Component
public class GroqWordRelationChecker implements WordRelationChecker {

    private static final Logger log = LoggerFactory.getLogger(GroqWordRelationChecker.class);
    private static final URI GROQ_ENDPOINT = URI.create("https://api.groq.com/openai/v1/chat/completions");
    private static final String MODEL = "llama-3.1-8b-instant";

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
                + "does. The chain must stay interesting: a good link is SPECIFIC to these two exact words, not "
                + "to the broad category they both happen to belong to. Score generously for genuine, specific "
                + "associations: synonyms and near-synonyms, cause-and-effect pairs (rain / umbrella), "
                + "part-whole pairs (wheel / car), strong typical co-occurrence or context (beach / sunscreen), "
                + "tool-and-user or tool-and-purpose (needle / sew), direct opposites (hot / cold), and "
                + "well-known cultural, idiomatic, or pop-culture associations (Batman / Gotham). Score LOW "
                + "(under 30) when the only link is that both words are instances of the same generic "
                + "category with no direct connection to each other — e.g. \"garaje\" and \"biblioteca\" are "
                + "both merely \"a place\", \"perro\" and \"gato\" are both merely \"an animal\", \"rojo\" and "
                + "\"azul\" are both merely \"a color\". Sharing a broad category is not enough on its own; "
                + "there must be a real, specific reason the two exact words connect. A pair only deserves a "
                + "high score when a reasonable person would name a concrete, non-generic reason for the link, "
                + "even if that reason is loose, indirect, or associative rather than a strict synonym. "
                + "Whenever you score low specifically because the only link is a shared generic category, "
                + "say so plainly in the reason (e.g. \"both are just places, no direct link\") instead of a "
                + "vague justification, so the player understands why it was too generic. "
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
