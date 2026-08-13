package com.kawser.cleanspringbootproject.secretmode.infrastructure.adapter.out.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kawser.cleanspringbootproject.game.domain.model.GameLanguage;
import com.kawser.cleanspringbootproject.secretmode.application.port.out.SecretQualityCheckException;
import com.kawser.cleanspringbootproject.secretmode.application.port.out.SecretQualityChecker;
import com.kawser.cleanspringbootproject.secretmode.domain.model.SolutionMatchLevel;
import org.slf4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Shared request/response mechanics for secretmode's AI judge adapters
 * (Groq, OpenRouter, ...), mirroring
 * game.infrastructure.adapter.out.ai.OpenAiCompatibleWordRelationChecker's
 * shape exactly (same HttpClient/timeouts, same "subclass only supplies
 * endpoint/model/key" split) but kept as an independent class since the two
 * prompts/response shapes (word-chain relatedness vs hidden-quality
 * judgement) are unrelated questions.
 */
abstract class OpenAiCompatibleSecretQualityChecker implements SecretQualityChecker {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final URI endpoint;
    private final String model;
    private final String apiKey;

    protected OpenAiCompatibleSecretQualityChecker(URI endpoint, String model, String apiKey) {
        this.endpoint = endpoint;
        this.model = model;
        this.apiKey = apiKey;
    }

    protected abstract Logger log();

    protected abstract String providerName();

    protected abstract String apiKeyPropertyName();

    @Override
    public WordJudgementResult judgeWord(String word, String hiddenQuality, GameLanguage language) {
        String systemPrompt = "You are the judge for a hidden-quality guessing game. There is a secret quality "
                + "that a whole category of words share (e.g. \"countries whose name starts with the letter "
                + "A\"). The player submits one candidate word at a time, without knowing the secret quality, "
                + "and you must decide whether that word genuinely satisfies it.\n\n"
                + "ACCEPT only when the submitted word factually and unambiguously satisfies the secret quality "
                + "exactly as stated — do not be lenient or \"close enough\": if the quality requires a "
                + "specific letter, category, or property, the word must literally have it. REJECT anything "
                + "that does not satisfy it, including words that are merely thematically related but do not "
                + "meet the literal criterion.\n\n"
                + "The submitted word may contain a minor spelling variation already corrected upstream — "
                + "judge the word's meaning, not its exact casing/accents.\n\n"
                + "Reply with ONLY compact JSON: {\"accepted\": <true|false>, \"reason\": \"<short, specific, "
                + "required, written in " + replyLanguage(language) + ">\"}. No markdown, no extra text.";
        String userPrompt = "Secret quality: \"" + hiddenQuality + "\"\nCandidate word: \"" + word + "\"";

        JsonNode payload = call(systemPrompt, userPrompt, 150, word, hiddenQuality);
        boolean accepted = payload.path("accepted").asBoolean(false);
        String reason = textOrNull(payload.path("reason"));
        return new WordJudgementResult(accepted, reason);
    }

    @Override
    public QualityMatchResult judgeFormulation(String formulationText, String hiddenQuality, GameLanguage language) {
        String systemPrompt = "You are the judge for the final guess of a hidden-quality guessing game. There "
                + "is a secret quality that a whole category of words share (stated precisely, e.g. \"countries "
                + "whose name starts with the letter A\"). The player, who never saw the secret quality "
                + "directly, now writes their own guess of what they think it is, in natural language.\n\n"
                + "Classify the guess into exactly one of three categories:\n"
                + "- \"CORRECT\": the guess is functionally equivalent to the real quality, even with different "
                + "wording or a different language (e.g. real=\"countries that start with the letter A\", "
                + "guess=\"paises que empiezan con A\" -> CORRECT).\n"
                + "- \"PARTIAL\": the guess captures the core idea but adds a constraint the real quality does "
                + "not have (e.g. adds \"African countries starting with A\" when there is no continent "
                + "restriction, which would wrongly exclude valid members like Australia), or only captures "
                + "part of a compound quality, or is otherwise close but not exactly equivalent.\n"
                + "- \"INCORRECT\": the guess is unrelated, contradicts the real quality, or is far too "
                + "broad/narrow to be considered close.\n\n"
                + "IMPORTANT: this judgement is final and binding for ending the game — do not soften your "
                + "classification to be encouraging. Do not include any numeric score in your answer.\n\n"
                + "\"reason\" must briefly explain what matches and what doesn't, written in "
                + replyLanguage(language) + ".\n\n"
                + "Reply with ONLY compact JSON: {\"matchLevel\": \"<CORRECT|PARTIAL|INCORRECT>\", \"reason\": "
                + "\"<short, specific, required>\"}. No markdown, no extra text.";
        String userPrompt = "Real secret quality: \"" + hiddenQuality + "\"\nPlayer's guess: \"" + formulationText + "\"";

        JsonNode payload = call(systemPrompt, userPrompt, 200, formulationText, hiddenQuality);
        SolutionMatchLevel matchLevel = parseMatchLevel(payload.path("matchLevel").asText(""));
        String reason = textOrNull(payload.path("reason"));
        return new QualityMatchResult(matchLevel, reason);
    }

    private SolutionMatchLevel parseMatchLevel(String raw) {
        try {
            return SolutionMatchLevel.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            log().warn("{} returned an unrecognized matchLevel '{}'; treating as INCORRECT", providerName(), raw);
            return SolutionMatchLevel.INCORRECT;
        }
    }

    private JsonNode call(String systemPrompt, String userPrompt, int maxTokens, String subject, String hiddenQuality) {
        if (apiKey == null || apiKey.isBlank()) {
            log().warn("{} is not configured; cannot judge '{}' against '{}'", apiKeyPropertyName(), subject, hiddenQuality);
            throw new SecretQualityCheckException(apiKeyPropertyName() + " is not configured");
        }
        try {
            String requestBody = objectMapper.writeValueAsString(chatRequest(systemPrompt, userPrompt, maxTokens));
            HttpRequest request = HttpRequest.newBuilder(endpoint)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log().warn("{} API returned status {} for '{}': {}",
                        providerName(), response.statusCode(), subject, response.body());
                throw new SecretQualityCheckException(providerName() + " API returned status " + response.statusCode());
            }
            JsonNode root = objectMapper.readTree(response.body());
            String content = root.path("choices").path(0).path("message").path("content").asText();
            return objectMapper.readTree(content);
        } catch (SecretQualityCheckException e) {
            throw e;
        } catch (Exception e) {
            log().warn("{} API call failed for '{}'", providerName(), subject, e);
            throw new SecretQualityCheckException(providerName() + " API call failed", e);
        }
    }

    private static String replyLanguage(GameLanguage language) {
        return language == GameLanguage.SPANISH ? "Spanish" : "English";
    }

    private static String textOrNull(JsonNode node) {
        String text = node.asText(null);
        return text == null || text.isBlank() ? null : text;
    }

    private Object chatRequest(String systemPrompt, String userPrompt, int maxTokens) {
        return new ChatRequest(
                model,
                new ChatMessage[] {
                    new ChatMessage("system", systemPrompt),
                    new ChatMessage("user", userPrompt)
                },
                0.0,
                maxTokens,
                new ResponseFormat("json_object"));
    }

    private record ChatRequest(String model, ChatMessage[] messages, double temperature, int max_tokens,
            ResponseFormat response_format) {
    }

    private record ChatMessage(String role, String content) {
    }

    private record ResponseFormat(String type) {
    }
}
