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
        String systemPrompt = "Word-chain game judge. Score how strongly two words are associated, the way a "
                + "human free-associates in casual play — not academic or thesaurus-level rigor. "
                + "ACCEPT (score >=50) when there's a direct link a person would name in one step: synonyms, "
                + "opposites, cause/effect (lluvia/paraguas), part/whole (rueda/coche), tool/purpose "
                + "(aguja/coser), typical pairing (playa/protector solar), well-known cultural association "
                + "(Batman/Gotham), or same specific real-world category — species, profession, food type, "
                + "sport, emotion, body part, etc. (perro/humano -> both animals; medico/profesor -> both "
                + "professions; alegria/tristeza -> both emotions). Category links count even without a "
                + "concrete real-world interaction between the two words, as long as the shared category is a "
                + "recognizable, bounded concept, not everything-is-this. "
                + "REJECT (score <50) when the only link is a broad near-universal category that fits almost "
                + "anything (\"objects\", \"places\", \"things\"), or when the connection only works by "
                + "skipping unstated intermediate steps a chain would normally require (humano/coche needs "
                + "humano-persona-trabajo-conductor-coche in between; score that pair LOW even though a human "
                + "can drive a car). If you're not sure whether it's one direct step or a hidden multi-step "
                + "chain, treat it as multi-step and score LOW. "
                + "Use fine-grained integers across the full 0-100 range, not round multiples of 5 or 10; vary "
                + "the exact number pair to pair. "
                + "\"reason\" must name the specific link in a few words (or say there isn't one), never a "
                + "vague phrase like \"related\". "
                + "Reply with ONLY compact JSON: {\"percentage\": <0-100 integer>, \"reason\": \"<short, "
                + "specific, required>\"}, reason written in " + replyLanguage + ". No markdown, no extra text.";
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
