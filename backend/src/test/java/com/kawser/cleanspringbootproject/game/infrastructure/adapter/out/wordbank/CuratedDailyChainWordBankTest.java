package com.kawser.cleanspringbootproject.game.infrastructure.adapter.out.wordbank;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kawser.cleanspringbootproject.game.domain.model.GameLanguage;
import com.kawser.cleanspringbootproject.game.domain.model.WordSet;
import com.kawser.cleanspringbootproject.game.infrastructure.adapter.out.wordbank.CuratedDailyChainWordBank.DailyEntry;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CuratedDailyChainWordBankTest {

    private static Map<GameLanguage, List<DailyEntry>> calendarWithTodaysWords(String... words) {
        int today = LocalDate.now(ZoneOffset.UTC).getDayOfYear();
        DailyEntry todaysEntry = new DailyEntry(today, List.of(words));
        return Map.of(
                GameLanguage.SPANISH, List.of(todaysEntry),
                GameLanguage.ENGLISH, List.of(todaysEntry));
    }

    @Test
    void fullChainChainsFourWordsIntoThreePhases() {
        CuratedDailyChainWordBank bank = new CuratedDailyChainWordBank(
                calendarWithTodaysWords("candado", "fresa", "jirafa", "volcán"));

        List<WordSet> chain = bank.fullChain(GameLanguage.SPANISH, 3, 999L);

        assertThat(chain).hasSize(3);
        assertThat(chain.get(0).startWord()).isEqualTo("candado");
        assertThat(chain.get(0).groupTargetWord()).isEqualTo("fresa");
        assertThat(chain.get(1).startWord()).isEqualTo("fresa");
        assertThat(chain.get(1).groupTargetWord()).isEqualTo("jirafa");
        assertThat(chain.get(2).startWord()).isEqualTo("jirafa");
        assertThat(chain.get(2).groupTargetWord()).isEqualTo("volcán");
    }

    @Test
    void everyPhasesInfiltratorTargetIsNeverReachableByARealSubmission() {
        // A solo daily room never has an infiltrator (see class javadoc), so
        // reaching WordSet.infiltratorTargetWord must be structurally
        // impossible for anything a player could actually type — otherwise
        // correctly completing a phase could be misread by
        // Round.reachesInfiltratorTarget as losing to a nonexistent
        // infiltrator.
        CuratedDailyChainWordBank bank = new CuratedDailyChainWordBank(
                calendarWithTodaysWords("candado", "fresa", "jirafa", "volcán"));

        List<WordSet> chain = bank.fullChain(GameLanguage.SPANISH, 3, 0L);

        for (WordSet wordSet : chain) {
            String trimmedRealisticInput = wordSet.groupTargetWord().trim();
            assertThat(trimmedRealisticInput).isNotEqualToIgnoringCase(wordSet.infiltratorTargetWord().trim());
            assertThat(wordSet.infiltratorTargetWord()).contains("\n");
        }
    }

    @Test
    void fullChainIgnoresTheSeedAndUsesTheCalendarEntryForTodayRegardless() {
        CuratedDailyChainWordBank bank = new CuratedDailyChainWordBank(
                calendarWithTodaysWords("candado", "fresa", "jirafa", "volcán"));

        List<WordSet> first = bank.fullChain(GameLanguage.SPANISH, 3, 1L);
        List<WordSet> second = bank.fullChain(GameLanguage.SPANISH, 3, 999999L);

        assertThat(first).isEqualTo(second);
    }

    @Test
    void fullChainPicksTheCalendarForTheRequestedLanguage() {
        int today = LocalDate.now(ZoneOffset.UTC).getDayOfYear();
        Map<GameLanguage, List<DailyEntry>> calendar = Map.of(
                GameLanguage.SPANISH, List.of(new DailyEntry(today, List.of("candado", "fresa"))),
                GameLanguage.ENGLISH, List.of(new DailyEntry(today, List.of("padlock", "strawberry"))));
        CuratedDailyChainWordBank bank = new CuratedDailyChainWordBank(calendar);

        List<WordSet> spanish = bank.fullChain(GameLanguage.SPANISH, 1, 0L);
        List<WordSet> english = bank.fullChain(GameLanguage.ENGLISH, 1, 0L);

        assertThat(spanish.get(0).startWord()).isEqualTo("candado");
        assertThat(english.get(0).startWord()).isEqualTo("padlock");
    }

    @Test
    void dayIndexWrapsWithModuloSoAnyCalendarSizeAlwaysResolvesAnEntry() {
        // A single-entry calendar must still resolve today's phases no
        // matter what day of the year it actually is (e.g. day 366 of a
        // leap year with a 365-entry calendar), since the lookup wraps with
        // modulo rather than indexing the day of year directly.
        DailyEntry onlyEntry = new DailyEntry(1, List.of("candado", "fresa"));
        CuratedDailyChainWordBank bank = new CuratedDailyChainWordBank(
                Map.of(GameLanguage.SPANISH, List.of(onlyEntry), GameLanguage.ENGLISH, List.of(onlyEntry)));

        List<WordSet> chain = bank.fullChain(GameLanguage.SPANISH, 1, 0L);

        assertThat(chain.get(0).startWord()).isEqualTo("candado");
        assertThat(chain.get(0).groupTargetWord()).isEqualTo("fresa");
    }

    @Test
    void nextPhaseWordSetFindsThePhaseThatStartsWithTheGivenWord() {
        CuratedDailyChainWordBank bank = new CuratedDailyChainWordBank(
                calendarWithTodaysWords("candado", "fresa", "jirafa", "volcán"));

        WordSet next = bank.nextPhaseWordSet(GameLanguage.SPANISH, "fresa", Set.of("candado"));

        assertThat(next.startWord()).isEqualTo("fresa");
        assertThat(next.groupTargetWord()).isEqualTo("jirafa");
    }

    @Test
    void firstWordSetReturnsTheFirstPhaseOfTodaysEntry() {
        CuratedDailyChainWordBank bank = new CuratedDailyChainWordBank(
                calendarWithTodaysWords("candado", "fresa", "jirafa", "volcán"));

        WordSet first = bank.firstWordSet(GameLanguage.SPANISH);

        assertThat(first.startWord()).isEqualTo("candado");
        assertThat(first.groupTargetWord()).isEqualTo("fresa");
    }

    @Test
    void constructorRejectsAnEmptyCalendarForAnyLanguage() {
        DailyEntry entry = new DailyEntry(1, List.of("candado", "fresa"));
        assertThrows(IllegalStateException.class, () -> new CuratedDailyChainWordBank(
                Map.of(GameLanguage.SPANISH, List.of(entry), GameLanguage.ENGLISH, List.of())));
    }

    @Test
    void realSpanishCalendarResourceLoadsAndYieldsThreeNonBlankPhases() {
        CuratedDailyChainWordBank bank = new CuratedDailyChainWordBank(new ObjectMapper());

        List<WordSet> spanishChain = bank.fullChain(GameLanguage.SPANISH, 3, 0L);

        assertThat(spanishChain).hasSize(3);
        for (WordSet wordSet : spanishChain) {
            assertThat(wordSet.startWord()).isNotBlank();
            assertThat(wordSet.groupTargetWord()).isNotBlank();
            assertThat(wordSet.infiltratorTargetWord()).isNotBlank();
        }
        // Each phase's start must be the previous phase's own target, per
        // ChainWordBank.fullChain's contract (see its javadoc).
        assertThat(spanishChain.get(1).startWord()).isEqualTo(spanishChain.get(0).groupTargetWord());
        assertThat(spanishChain.get(2).startWord()).isEqualTo(spanishChain.get(1).groupTargetWord());
    }
}
