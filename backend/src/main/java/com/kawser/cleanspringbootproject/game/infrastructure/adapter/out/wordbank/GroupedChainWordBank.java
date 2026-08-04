package com.kawser.cleanspringbootproject.game.infrastructure.adapter.out.wordbank;

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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Deals chain phases from a wordbank curated into named thematic groups
 * (resources/wordbank/nouns-{en,es}.json), rather than scoring words by
 * embedding distance: the start word and both target words are always drawn
 * from three different groups, which stands in for "semantically far" at
 * effectively zero cost (no model to load, no inference call per word). A
 * word only needs to look unrelated to the two target words it's paired
 * with, not to every other word in the bank, so picking targets from
 * disjoint categories is enough to keep chains hard to bridge without
 * needing an actual distance metric.
 */
@Component
public class GroupedChainWordBank implements ChainWordBank {

    private static final Map<GameLanguage, String> RESOURCE_BY_LANGUAGE = Map.of(
            GameLanguage.ENGLISH, "wordbank/nouns-en.json",
            GameLanguage.SPANISH, "wordbank/nouns-es.json");

    private final Map<GameLanguage, Map<String, List<String>>> groupsByLanguage;
    private final Random random;

    @Autowired
    public GroupedChainWordBank(ObjectMapper objectMapper) {
        this(RESOURCE_BY_LANGUAGE.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> loadGroups(objectMapper, entry.getValue()))),
                new Random());
    }

    /** Package-visible for tests: lets fixed word groups and Random be injected directly. */
    GroupedChainWordBank(Map<GameLanguage, Map<String, List<String>>> groupsByLanguage, Random random) {
        this.groupsByLanguage = groupsByLanguage;
        this.random = random;
    }

    private static Map<String, List<String>> loadGroups(ObjectMapper objectMapper, String resourcePath) {
        try (InputStream inputStream = new ClassPathResource(resourcePath).getInputStream()) {
            return objectMapper.readValue(inputStream, new TypeReference<LinkedHashMap<String, List<String>>>() {
            });
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load word bank from " + resourcePath, e);
        }
    }

    @Override
    public WordSet firstWordSet(GameLanguage language) {
        Map<String, List<String>> groups = groupsByLanguage.get(language);
        String startGroup = pickRandomGroup(groups, Set.of());
        String startWord = pickRandomWord(groups.get(startGroup), Set.of());

        String groupTargetGroup = pickRandomGroup(groups, Set.of(startGroup));
        String groupTargetWord = pickRandomWord(groups.get(groupTargetGroup), Set.of(normalize(startWord)));

        String infiltratorTargetGroup = pickRandomGroup(groups, Set.of(startGroup, groupTargetGroup));
        String infiltratorTargetWord = pickRandomWord(
                groups.get(infiltratorTargetGroup), Set.of(normalize(startWord), normalize(groupTargetWord)));

        return new WordSet(startWord, groupTargetWord, infiltratorTargetWord);
    }

    @Override
    public WordSet nextPhaseWordSet(GameLanguage language, String startWord, Set<String> usedWords) {
        Map<String, List<String>> groups = groupsByLanguage.get(language);
        Set<String> exclude = usedWords.stream().map(GroupedChainWordBank::normalize)
                .collect(Collectors.toCollection(HashSet::new));
        exclude.add(normalize(startWord));

        String startGroup = groupContaining(groups, startWord);

        String groupTargetGroup = pickRandomGroup(groups, excludeGroups(startGroup));
        String groupTargetWord = pickRandomWord(groups.get(groupTargetGroup), exclude);

        Set<String> excludeWithGroupTarget = new HashSet<>(exclude);
        excludeWithGroupTarget.add(normalize(groupTargetWord));
        String infiltratorTargetGroup = pickRandomGroup(groups, excludeGroups(startGroup, groupTargetGroup));
        String infiltratorTargetWord = pickRandomWord(groups.get(infiltratorTargetGroup), excludeWithGroupTarget);

        if (infiltratorTargetWord.equalsIgnoreCase(groupTargetWord)) {
            // Degenerate case: every eligible group is exhausted down to the
            // same leftover word. Force any other distinct word rather than
            // let WordSet's constructor reject the pair outright.
            infiltratorTargetWord = groups.values().stream().flatMap(List::stream)
                    .filter(w -> !normalize(w).equals(normalize(groupTargetWord)))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "word bank for " + language + " needs at least 2 distinct words"));
        }
        return new WordSet(startWord, groupTargetWord, infiltratorTargetWord);
    }

    @Override
    public List<WordSet> fullChain(GameLanguage language, int phaseCount, long seed) {
        return new GroupedChainWordBank(groupsByLanguage, new Random(seed)).fullChain(language, phaseCount);
    }

    private String groupContaining(Map<String, List<String>> groups, String word) {
        String normalized = normalize(word);
        return groups.entrySet().stream()
                .filter(entry -> entry.getValue().stream().anyMatch(w -> normalize(w).equals(normalized)))
                .map(Map.Entry::getKey)
                .findFirst()
                // The reference word may have come from a previous phase's
                // group target, which is always drawn from the wordbank, so
                // this only trips if the bank itself changed underneath a
                // live game.
                .orElseThrow(() -> new IllegalStateException("word \"" + word + "\" not found in any word group"));
    }

    private static Set<String> excludeGroups(String... groupNames) {
        return Set.of(groupNames);
    }

    /** Picks a uniformly random group name, excluding the given ones (falls back to the full set if that would leave none). */
    private String pickRandomGroup(Map<String, List<String>> groups, Set<String> excludeGroupNames) {
        List<String> eligible = groups.keySet().stream()
                .filter(name -> !excludeGroupNames.contains(name))
                .toList();
        if (eligible.isEmpty()) {
            eligible = List.copyOf(groups.keySet());
        }
        return eligible.get(random.nextInt(eligible.size()));
    }

    /** Picks a uniformly random word from the pool, excluding already-used words (falls back to the full pool if that would leave none). */
    private String pickRandomWord(List<String> pool, Set<String> excludeWords) {
        List<String> eligible = pool.stream().filter(w -> !excludeWords.contains(normalize(w))).toList();
        if (eligible.isEmpty()) {
            eligible = pool;
        }
        return eligible.get(random.nextInt(eligible.size()));
    }

    private static String normalize(String word) {
        return word.trim().toLowerCase();
    }
}
