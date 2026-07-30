package com.kawser.cleanspringbootproject.game.infrastructure.adapter.out.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kawser.cleanspringbootproject.game.application.port.out.ChainWordBank;
import com.kawser.cleanspringbootproject.game.domain.model.GameLanguage;
import com.kawser.cleanspringbootproject.game.domain.model.WordSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Deals chain phases by picking words from a flat, curated list of common
 * nouns per language (resources/wordbank/nouns-{en,es}.json) and choosing
 * targets that are semantically FAR from the reference word: a handful of
 * random candidates are sampled from the eligible pool and whichever one
 * has the lowest cosine similarity (via the shared {@link
 * SentenceEmbeddingModel}) to the reference word wins - the opposite of
 * WordRelationChecker's job of finding words that ARE related. This makes
 * each phase's start/target pair deliberately hard to bridge.
 *
 * <p>Per-word embeddings are cached forever (the candidate pool is a small
 * fixed list, so this is bounded and safe): after a short warm-up period,
 * essentially every candidate comparison is a cache hit rather than a fresh
 * ONNX inference call. The cache is a singleton-bean field, so it is shared
 * across every room/game for the lifetime of the JVM.
 */
@Component
public class EmbeddingChainWordBank implements ChainWordBank {

    private static final int SAMPLE_SIZE = 8;

    private static final Map<GameLanguage, String> RESOURCE_BY_LANGUAGE = Map.of(
            GameLanguage.ENGLISH, "wordbank/nouns-en.json",
            GameLanguage.SPANISH, "wordbank/nouns-es.json");

    private final SentenceEmbeddingModel embeddingModel;
    private final Map<GameLanguage, List<String>> wordsByLanguage;
    private final Map<String, float[]> embeddingCache = new ConcurrentHashMap<>();
    private final Random random;

    @Autowired
    public EmbeddingChainWordBank(SentenceEmbeddingModel embeddingModel, ObjectMapper objectMapper) {
        this(embeddingModel,
                RESOURCE_BY_LANGUAGE.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, entry -> loadWords(objectMapper, entry.getValue()))),
                new Random());
    }

    /** Package-visible for tests: lets a fixed word list and Random be injected directly. */
    EmbeddingChainWordBank(
            SentenceEmbeddingModel embeddingModel, Map<GameLanguage, List<String>> wordsByLanguage, Random random) {
        this.embeddingModel = embeddingModel;
        this.wordsByLanguage = wordsByLanguage;
        this.random = random;
    }

    private static List<String> loadWords(ObjectMapper objectMapper, String resourcePath) {
        try (InputStream inputStream = new ClassPathResource(resourcePath).getInputStream()) {
            return objectMapper.readValue(inputStream, new TypeReference<List<String>>() {
            });
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load word bank from " + resourcePath, e);
        }
    }

    @Override
    public WordSet firstWordSet(GameLanguage language) {
        List<String> words = wordsByLanguage.get(language);
        String startWord = words.get(random.nextInt(words.size()));

        String groupTargetWord = pickMostDissimilar(language, startWord, Set.of(normalize(startWord)));
        String infiltratorTargetWord = pickMostDissimilar(
                language, startWord, Set.of(normalize(startWord), normalize(groupTargetWord)));
        if (infiltratorTargetWord.equalsIgnoreCase(groupTargetWord)) {
            infiltratorTargetWord = wordsByLanguage.get(language).stream()
                    .filter(w -> !normalize(w).equals(normalize(groupTargetWord)) && !normalize(w).equals(normalize(startWord)))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "word bank for " + language + " needs at least 3 distinct words"));
        }
        return new WordSet(startWord, groupTargetWord, infiltratorTargetWord);
    }

    /**
     * Deals a fully deterministic chain for the given seed: builds a
     * throw-away sibling instance sharing this bean's word lists and
     * embedding cache (both read-only/append-only and safe to share) but
     * backed by its own fresh {@code Random(seed)}, so a seeded call never
     * disturbs the shared instance field that concurrent live rooms keep
     * drawing from.
     */
    @Override
    public List<WordSet> fullChain(GameLanguage language, int phaseCount, long seed) {
        return new EmbeddingChainWordBank(embeddingModel, wordsByLanguage, new Random(seed))
                .fullChain(language, phaseCount);
    }

    @Override
    public WordSet nextPhaseWordSet(GameLanguage language, String startWord, Set<String> usedWords) {
        Set<String> exclude = usedWords.stream().map(EmbeddingChainWordBank::normalize)
                .collect(Collectors.toCollection(HashSet::new));
        exclude.add(normalize(startWord));

        String groupTargetWord = pickMostDissimilar(language, startWord, exclude);
        Set<String> excludeWithGroupTarget = new HashSet<>(exclude);
        excludeWithGroupTarget.add(normalize(groupTargetWord));
        String infiltratorTargetWord = pickMostDissimilar(language, startWord, excludeWithGroupTarget);
        if (infiltratorTargetWord.equalsIgnoreCase(groupTargetWord)) {
            // Degenerate case: even the exhausted-pool fallback in
            // pickMostDissimilar landed on the same word twice (only
            // possible with a pathologically small word list). Force any
            // other distinct word rather than let WordSet's constructor
            // reject the pair outright.
            infiltratorTargetWord = wordsByLanguage.get(language).stream()
                    .filter(w -> !normalize(w).equals(normalize(groupTargetWord)))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "word bank for " + language + " needs at least 2 distinct words"));
        }
        return new WordSet(startWord, groupTargetWord, infiltratorTargetWord);
    }

    /**
     * Samples up to SAMPLE_SIZE random candidates (excluding already-used
     * words) and returns whichever one is least similar to referenceWord.
     * Falls back to ignoring the exclude set if it would leave no eligible
     * candidates at all, rather than failing a live game outright.
     */
    private String pickMostDissimilar(GameLanguage language, String referenceWord, Set<String> exclude) {
        List<String> pool = wordsByLanguage.get(language);
        List<String> eligible = pool.stream().filter(w -> !exclude.contains(normalize(w))).toList();
        if (eligible.isEmpty()) {
            eligible = pool;
        }

        List<String> sample = sampleCandidates(eligible, Math.min(SAMPLE_SIZE, eligible.size()));
        float[] referenceEmbedding = embeddingFor(referenceWord);

        String best = null;
        double lowestSimilarity = Double.POSITIVE_INFINITY;
        for (String candidate : sample) {
            double similarity = SentenceEmbeddingModel.cosineSimilarity(referenceEmbedding, embeddingFor(candidate));
            if (similarity < lowestSimilarity) {
                lowestSimilarity = similarity;
                best = candidate;
            }
        }
        return best;
    }

    private List<String> sampleCandidates(List<String> eligible, int count) {
        List<String> shuffled = new ArrayList<>(eligible);
        Collections.shuffle(shuffled, random);
        return shuffled.subList(0, count);
    }

    private float[] embeddingFor(String word) {
        return embeddingCache.computeIfAbsent(normalize(word), key -> embeddingModel.embed(word));
    }

    private static String normalize(String word) {
        return word.trim().toLowerCase();
    }
}
