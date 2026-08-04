package com.kawser.cleanspringbootproject.game.infrastructure.adapter.out.wordbank;

import com.kawser.cleanspringbootproject.game.domain.model.GameLanguage;
import com.kawser.cleanspringbootproject.game.domain.model.WordSet;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GroupedChainWordBankTest {

    private static final Map<GameLanguage, Map<String, List<String>>> GROUPS = Map.of(
            GameLanguage.ENGLISH, Map.of(
                    "fruit", List.of("apple", "banana", "orange"),
                    "animals", List.of("cat", "dog", "horse"),
                    "furniture", List.of("table", "chair", "sofa")));

    @Test
    void firstWordSetDrawsStartAndBothTargetsFromDifferentGroups() {
        GroupedChainWordBank bank = new GroupedChainWordBank(GROUPS, new Random(1));

        WordSet result = bank.firstWordSet(GameLanguage.ENGLISH);

        assertThat(result.startWord()).isNotEqualTo(result.groupTargetWord());
        assertThat(result.groupTargetWord()).isNotEqualToIgnoringCase(result.infiltratorTargetWord());
        assertThat(groupOf(result.startWord())).isNotEqualTo(groupOf(result.groupTargetWord()));
        assertThat(groupOf(result.startWord())).isNotEqualTo(groupOf(result.infiltratorTargetWord()));
        assertThat(groupOf(result.groupTargetWord())).isNotEqualTo(groupOf(result.infiltratorTargetWord()));
    }

    @Test
    void nextPhaseWordSetKeepsTheGivenStartWordAndDrawsTargetsFromOtherGroups() {
        GroupedChainWordBank bank = new GroupedChainWordBank(GROUPS, new Random(1));

        WordSet result = bank.nextPhaseWordSet(GameLanguage.ENGLISH, "cat", Set.of());

        assertThat(result.startWord()).isEqualTo("cat");
        assertThat(groupOf(result.groupTargetWord())).isNotEqualTo("animals");
        assertThat(groupOf(result.infiltratorTargetWord())).isNotEqualTo("animals");
        assertThat(groupOf(result.groupTargetWord())).isNotEqualTo(groupOf(result.infiltratorTargetWord()));
    }

    @Test
    void nextPhaseWordSetNeverReturnsAWordAlreadyUsed() {
        GroupedChainWordBank bank = new GroupedChainWordBank(GROUPS, new Random(1));

        WordSet result = bank.nextPhaseWordSet(GameLanguage.ENGLISH, "cat", Set.of("banana", "orange"));

        assertThat(result.groupTargetWord()).isNotEqualToIgnoringCase("banana");
        assertThat(result.groupTargetWord()).isNotEqualToIgnoringCase("orange");
        assertThat(result.infiltratorTargetWord()).isNotEqualToIgnoringCase("banana");
        assertThat(result.infiltratorTargetWord()).isNotEqualToIgnoringCase("orange");
    }

    @Test
    void fullChainWithTheSameSeedIsFullyDeterministic() {
        GroupedChainWordBank bank = new GroupedChainWordBank(GROUPS, new Random());

        List<WordSet> first = bank.fullChain(GameLanguage.ENGLISH, 3, 42L);
        List<WordSet> second = bank.fullChain(GameLanguage.ENGLISH, 3, 42L);

        assertThat(first).isEqualTo(second);
    }

    @Test
    void fullChainNeverRepeatsAWordAcrossPhases() {
        GroupedChainWordBank bank = new GroupedChainWordBank(GROUPS, new Random(7));

        List<WordSet> chain = bank.fullChain(GameLanguage.ENGLISH, 3);

        Set<String> seen = new HashSet<>();
        for (WordSet wordSet : chain) {
            assertThat(seen.add(wordSet.groupTargetWord().toLowerCase())).isTrue();
            assertThat(seen.add(wordSet.infiltratorTargetWord().toLowerCase())).isTrue();
        }
    }

    private static String groupOf(String word) {
        return GROUPS.get(GameLanguage.ENGLISH).entrySet().stream()
                .filter(entry -> entry.getValue().stream().anyMatch(w -> w.equalsIgnoreCase(word)))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow();
    }
}
