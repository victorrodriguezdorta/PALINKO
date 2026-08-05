package com.kawser.cleanspringbootproject.game.infrastructure.adapter.out.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kawser.cleanspringbootproject.game.application.port.out.WordRelation;
import com.kawser.cleanspringbootproject.game.application.port.out.WordRelationCheckException;
import com.kawser.cleanspringbootproject.game.application.port.out.WordRelationChecker;
import com.kawser.cleanspringbootproject.game.domain.model.GameLanguage;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.slf4j.Logger;

/**
 * Shared request/response mechanics for word-relatedness adapters backed by
 * an OpenAI-compatible chat-completions endpoint (Groq, OpenRouter, ...).
 * Subclasses only need to supply the endpoint, model id, API key, and a
 * logger — the prompt, request shape, and JSON parsing are identical across
 * providers because they're all speaking the same wire format.
 */
abstract class OpenAiCompatibleWordRelationChecker implements WordRelationChecker {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final URI endpoint;
    private final String model;
    private final String apiKey;

    protected OpenAiCompatibleWordRelationChecker(URI endpoint, String model, String apiKey) {
        this.endpoint = endpoint;
        this.model = model;
        this.apiKey = apiKey;
    }

    protected abstract Logger log();

    protected abstract String providerName();

    protected abstract String apiKeyPropertyName();

    @Override
    public WordRelation relatedness(String wordA, String wordB, GameLanguage language) {
        String a = wordA.trim();
        String b = wordB.trim();
        if (a.equalsIgnoreCase(b)) {
            return new WordRelation(100, null);
        }
        if (apiKey == null || apiKey.isBlank()) {
            log().warn("{} is not configured; cannot judge relatedness for '{}' / '{}'", apiKeyPropertyName(), a, b);
            throw new WordRelationCheckException(apiKeyPropertyName() + " is not configured");
        }

        try {
            String requestBody = objectMapper.writeValueAsString(chatRequest(a, b, language));
            HttpRequest request = HttpRequest.newBuilder(endpoint)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log().warn("{} API returned status {} for '{}' / '{}': {}",
                        providerName(), response.statusCode(), a, b, response.body());
                throw new WordRelationCheckException(providerName() + " API returned status " + response.statusCode());
            }
            return parseRelation(response.body());
        } catch (WordRelationCheckException e) {
            throw e;
        } catch (Exception e) {
            log().warn("{} API call failed for '{}' / '{}'", providerName(), a, b, e);
            throw new WordRelationCheckException(providerName() + " API call failed", e);
        }
    }

    private Object chatRequest(String wordA, String wordB, GameLanguage language) {
        String replyLanguage = language == GameLanguage.SPANISH ? "Spanish" : "English";
        String systemPrompt = "You judge how related two words or short phrases are, for a word-chain party "
                + "game where players connect words the way a human free-associates, not the way a thesaurus "
                + "does. Be a STRICT judge, not a lenient one: most pairs of random words are NOT meaningfully "
                + "related, and the default score for an arbitrary pair should be low. A good link is SPECIFIC "
                + "to these two exact words, not to some broad category they both happen to belong to, and not "
                + "a vague, stretched, or \"if you squint\" association. "
                + "Score HIGH (70-100) only for genuine, specific, direct associations that most people would "
                + "immediately recognize: synonyms and near-synonyms, direct cause-and-effect pairs (rain / "
                + "umbrella), part-whole pairs (wheel / car), strong and specific typical co-occurrence (beach "
                + "/ sunscreen), a tool and its direct purpose (needle / sew), direct opposites (hot / cold), "
                + "or a well-known, unambiguous cultural or pop-culture association (Batman / Gotham). "
                + "Score MEDIUM (30-69) only when there is a real but weaker or more indirect specific "
                + "connection you could still name concretely. "
                + "Score LOW (0-29) for everything else, including: pairs whose only link is sharing a broad "
                + "generic category with no direct connection between the two exact words — e.g. \"garaje\" "
                + "and \"biblioteca\" are both merely \"a place\", \"perro\" and \"gato\" are both merely \"an "
                + "animal\", \"rojo\" and \"azul\" are both merely \"a color\"; pairs where the connection is "
                + "vague, requires several inferential steps, or only makes sense with a very generous or "
                + "creative reading; and pairs with no real connection at all. When in doubt between two score "
                + "bands, pick the LOWER one — do not round up to be generous. "
                + "You MUST always explain your score: for every single answer, the \"reason\" field must name "
                + "the SPECIFIC, concrete connection between these two exact words (or state plainly that there "
                + "isn't one and only a shared generic category / no real link exists). Never give a vague or "
                + "generic justification like \"they are related\" or \"somewhat connected\" — always say "
                + "exactly how or why, in a few words. "
                + "Reply with ONLY compact JSON: {\"percentage\": <0-100 integer>, \"reason\": \"<short but "
                + "specific justification, required, never empty>\"}. The \"reason\" text must be written in "
                + replyLanguage + ". No markdown, no extra text.";
        String userPrompt = "Word A: \"" + wordA + "\"\nWord B: \"" + wordB + "\"";

        return new ChatRequest(
                model,
                new ChatMessage[] {
                    new ChatMessage("system", systemPrompt),
                    new ChatMessage("user", userPrompt)
                },
                0.0,
                120,
                new ResponseFormat("json_object"));
    }

    private WordRelation parseRelation(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        String content = root.path("choices").path(0).path("message").path("content").asText();
        JsonNode payload = objectMapper.readTree(content);

        int percentage = Math.max(0, Math.min(100, payload.path("percentage").asInt(0)));
        String reason = payload.path("reason").asText(null);
        if (reason != null && reason.isBlank()) {
            reason = null;
        }
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
