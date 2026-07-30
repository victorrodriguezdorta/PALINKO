package com.kawser.cleanspringbootproject.game.infrastructure.adapter.out.ai;

import com.kawser.cleanspringbootproject.game.domain.model.GameLanguage;
import com.kawser.cleanspringbootproject.game.domain.model.WordSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Exercises the pure candidate-selection logic against a mocked {@link
 * SentenceEmbeddingModel} with hand-picked deterministic vectors, never the
 * real ~120MB ONNX model — mirroring the existing convention of not testing
 * live-model inference (see EmbeddingWordRelationChecker, also untested
 * against the real model).
 */
class EmbeddingChainWordBankTest {

    private SentenceEmbeddingModel embeddingModel;

    @BeforeEach
    void setUp() {
        embeddingModel = mock(SentenceEmbeddingModel.class);
        // Any word not explicitly stubbed below falls back to a neutral
        // vector identical to "start"'s, i.e. maximally similar to it — so
        // it never accidentally wins a "most dissimilar" comparison.
        when(embeddingModel.embed(anyString())).thenReturn(new float[]{1f, 0f});
    }

    private void embed(String word, float x, float y) {
        when(embeddingModel.embed(word)).thenReturn(new float[]{x, y});
    }

    @Test
    void firstWordSetPicksTheMostDissimilarCandidateOfTheSample() {
        // Three words positioned so that, whichever one random picks as the
        // start word, the algorithm's target choice is unambiguous: "far"
        // is always more dissimilar to either "a" or "b" than they are to
        // each other.
        embed("a", 1f, 0f);
        embed("b", 0.5f, 0.866f);
        embed("far", -1f, 0f);
        Map<GameLanguage, List<String>> words = Map.of(GameLanguage.ENGLISH, List.of("a", "b", "far"));
        EmbeddingChainWordBank bank = new EmbeddingChainWordBank(embeddingModel, words, new Random(1));

        WordSet result = bank.firstWordSet(GameLanguage.ENGLISH);

        assertThat(result.startWord()).isNotEqualTo(result.groupTargetWord());
        assertThat(result.groupTargetWord()).isNotEqualTo(result.infiltratorTargetWord());
        float[] startEmbedding = embeddingModel.embed(result.startWord());
        double groupSimilarity =
                SentenceEmbeddingModel.cosineSimilarity(startEmbedding, embeddingModel.embed(result.groupTargetWord()));
        double infiltratorSimilarity = SentenceEmbeddingModel.cosineSimilarity(
                startEmbedding, embeddingModel.embed(result.infiltratorTargetWord()));
        // groupTargetWord was chosen as the single most-dissimilar
        // candidate, so it can never be *more* similar to the start word
        // than whatever was left over for infiltratorTargetWord.
        assertThat(groupSimilarity).isLessThanOrEqualTo(infiltratorSimilarity);
    }

    @Test
    void nextPhaseWordSetPicksTheLeastSimilarCandidatesToTheGivenStartWord() {
        embed("alpha", 1f, 0f);
        embed("beta", 0.9f, 0.436f);
        embed("gamma", 0f, 1f);
        embed("delta", -1f, 0f);
        Map<GameLanguage, List<String>> words =
                Map.of(GameLanguage.ENGLISH, List.of("alpha", "beta", "gamma", "delta"));
        EmbeddingChainWordBank bank = new EmbeddingChainWordBank(embeddingModel, words, new Random(1));

        WordSet result = bank.nextPhaseWordSet(GameLanguage.ENGLISH, "alpha", Set.of());

        assertThat(result.startWord()).isEqualTo("alpha");
        assertThat(result.groupTargetWord()).isEqualTo("delta");
        assertThat(result.infiltratorTargetWord()).isEqualTo("gamma");
    }

    @Test
    void nextPhaseWordSetNeverReturnsAWordAlreadyUsed() {
        embed("alpha", 1f, 0f);
        embed("beta", 0.9f, 0.436f);
        embed("gamma", 0f, 1f);
        embed("delta", -1f, 0f);
        Map<GameLanguage, List<String>> words =
                Map.of(GameLanguage.ENGLISH, List.of("alpha", "beta", "gamma", "delta"));
        EmbeddingChainWordBank bank = new EmbeddingChainWordBank(embeddingModel, words, new Random(1));

        WordSet result = bank.nextPhaseWordSet(GameLanguage.ENGLISH, "alpha", Set.of("delta"));

        assertThat(result.groupTargetWord()).isNotEqualTo("delta");
        assertThat(result.infiltratorTargetWord()).isNotEqualTo("delta");
        assertThat(result.groupTargetWord()).isEqualTo("gamma");
        assertThat(result.infiltratorTargetWord()).isEqualTo("beta");
    }

    @Test
    void groupAndInfiltratorTargetsAreAlwaysMutuallyDistinct() {
        embed("alpha", 1f, 0f);
        embed("beta", 0.9f, 0.436f);
        embed("gamma", 0f, 1f);
        Map<GameLanguage, List<String>> words = Map.of(GameLanguage.ENGLISH, List.of("alpha", "beta", "gamma"));
        EmbeddingChainWordBank bank = new EmbeddingChainWordBank(embeddingModel, words, new Random(1));

        WordSet result = bank.nextPhaseWordSet(GameLanguage.ENGLISH, "alpha", Set.of());

        assertThat(result.groupTargetWord()).isNotEqualToIgnoringCase(result.infiltratorTargetWord());
    }

    @Test
    void fallsBackToAllowingARepeatRatherThanCrashingWhenThePoolIsExhausted() {
        embed("alpha", 1f, 0f);
        embed("beta", 0.8f, 0.2f);
        embed("delta", -1f, 0f);
        Map<GameLanguage, List<String>> words = Map.of(GameLanguage.ENGLISH, List.of("alpha", "beta", "delta"));
        EmbeddingChainWordBank bank = new EmbeddingChainWordBank(embeddingModel, words, new Random(1));

        // Exclude everything except the start word itself - forces the
        // exhausted-pool fallback for the group target pick.
        WordSet result = bank.nextPhaseWordSet(GameLanguage.ENGLISH, "alpha", Set.of("beta", "delta"));

        assertThat(result.startWord()).isEqualTo("alpha");
        assertThat(result.groupTargetWord()).isNotEqualToIgnoringCase(result.infiltratorTargetWord());
    }

    @Test
    void embeddingsAreCachedAcrossRepeatedSelections() {
        embed("alpha", 1f, 0f);
        embed("beta", 0.9f, 0.436f);
        embed("gamma", 0f, 1f);
        embed("delta", -1f, 0f);
        Map<GameLanguage, List<String>> words =
                Map.of(GameLanguage.ENGLISH, List.of("alpha", "beta", "gamma", "delta"));
        EmbeddingChainWordBank bank = new EmbeddingChainWordBank(embeddingModel, words, new Random(1));

        bank.nextPhaseWordSet(GameLanguage.ENGLISH, "alpha", Set.of());
        bank.nextPhaseWordSet(GameLanguage.ENGLISH, "alpha", Set.of());

        // Every word appears in both calls, yet each is only ever embedded
        // once thanks to the per-word cache.
        verify(embeddingModel, times(1)).embed("alpha");
        verify(embeddingModel, times(1)).embed("beta");
        verify(embeddingModel, times(1)).embed("gamma");
        verify(embeddingModel, times(1)).embed("delta");
    }

    @Test
    void fullChainWithTheSameSeedIsFullyDeterministic() {
        embed("alpha", 1f, 0f);
        embed("beta", 0.9f, 0.436f);
        embed("gamma", 0f, 1f);
        embed("delta", -1f, 0f);
        embed("epsilon", 0.7f, 0.7f);
        Map<GameLanguage, List<String>> words =
                Map.of(GameLanguage.ENGLISH, List.of("alpha", "beta", "gamma", "delta", "epsilon"));
        // Constructed with an arbitrary, unseeded Random: fullChain(seed)
        // must ignore this shared instance and instead reproduce the same
        // result across independent calls with the same seed, since it is
        // meant to back a deterministic daily challenge rather than the
        // live-room randomness this constructor's own Random represents.
        EmbeddingChainWordBank bank = new EmbeddingChainWordBank(embeddingModel, words, new Random());

        List<WordSet> first = bank.fullChain(GameLanguage.ENGLISH, 3, 42L);
        List<WordSet> second = bank.fullChain(GameLanguage.ENGLISH, 3, 42L);

        assertThat(first).isEqualTo(second);
    }

    @Test
    void fullChainWithDifferentSeedsCanPickADifferentStartWord() {
        embed("alpha", 1f, 0f);
        embed("beta", 0.9f, 0.436f);
        embed("gamma", 0f, 1f);
        embed("delta", -1f, 0f);
        embed("epsilon", 0.7f, 0.7f);
        Map<GameLanguage, List<String>> words =
                Map.of(GameLanguage.ENGLISH, List.of("alpha", "beta", "gamma", "delta", "epsilon"));
        EmbeddingChainWordBank bank = new EmbeddingChainWordBank(embeddingModel, words, new Random());

        // firstWordSet's start word is picked via random.nextInt(words.size())
        // directly (not an argmin over a shuffled sample like the target
        // picks are), so it's the one part of the chain guaranteed to vary
        // deterministically with the seed rather than just the shuffle order.
        Set<String> startWordsSeen = new HashSet<>();
        for (long seed = 0; seed < 5; seed++) {
            startWordsSeen.add(bank.fullChain(GameLanguage.ENGLISH, 1, seed).get(0).startWord());
        }

        assertThat(startWordsSeen).hasSizeGreaterThan(1);
    }

    @Test
    void wordsAreMatchedCaseInsensitivelyForExclusion() {
        embed("Alpha", 1f, 0f);
        embed("Beta", 0.9f, 0.436f);
        embed("Delta", -1f, 0f);
        Map<GameLanguage, List<String>> words = new HashMap<>();
        words.put(GameLanguage.ENGLISH, List.of("Alpha", "Beta", "Delta"));
        EmbeddingChainWordBank bank = new EmbeddingChainWordBank(embeddingModel, words, new Random(1));

        WordSet result = bank.nextPhaseWordSet(GameLanguage.ENGLISH, "Alpha", Set.of("delta"));

        assertThat(result.groupTargetWord()).isNotEqualToIgnoringCase("delta");
    }
}
