package com.kawser.cleanspringbootproject.game.infrastructure.adapter.out.wordbank;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kawser.cleanspringbootproject.game.application.port.out.ChainWordBank;
import com.kawser.cleanspringbootproject.game.domain.model.GameLanguage;
import com.kawser.cleanspringbootproject.game.domain.model.WordSet;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Deals daily-challenge phases from a hand-curated calendar
 * (resources/wordbank/daily-challenges-{es,en}.json) instead of the random
 * category-disjoint draw {@link GroupedChainWordBank} uses for regular
 * multiplayer rooms. Category separation alone lets through pairs that are
 * "different group" but still an easy bridge in practice — e.g. "candado"
 * (objects) and "casa" (buildings) connect trivially via "puerta" — which is
 * tolerable for a live room where players just get a slightly-too-easy
 * phase, but undermines the one mode (the daily challenge) meant to be a
 * deliberately hard, identical-for-everyone puzzle. So instead of trying to
 * approximate "unrelated" at random, every day's phases are picked by hand
 * ahead of time and shipped as data.
 *
 * <p>Each calendar entry maps one UTC calendar day (1-365) to exactly four
 * simple, everyday words forming a single chain: word 1 starts phase 1,
 * word 2 is phase 1's target and phase 2's start, word 3 is phase 2's
 * target and phase 3's start, word 4 is phase 3's target — three phases
 * from four words. The daily challenge is always a single-player room (see
 * GameApplicationService.createDailyRoom), so it never deals a real
 * infiltrator: a 1-player room's infiltrator count always clamps to zero
 * (see Room.maxInfiltratorCount), so nobody in Round.infiltratorPlayerIds
 * ever exists to be told apart from the crew. WordSet still requires a
 * non-blank infiltratorTargetWord distinct from groupTargetWord, so each
 * phase gets one that is structurally unreachable by any real player input
 * rather than a second curated word — see {@link #unusedInfiltratorTarget}.
 *
 * <p>Not used for regular (non-daily) rooms — {@link
 * com.kawser.cleanspringbootproject.game.infrastructure.config.GameApplicationConfig}
 * wires this bean only into {@code createDailyRoom}, leaving {@link
 * GroupedChainWordBank} as the sole word source for {@code startGame}.
 */
public class CuratedDailyChainWordBank implements ChainWordBank {

    private static final Map<GameLanguage, String> RESOURCE_BY_LANGUAGE = Map.of(
            GameLanguage.SPANISH, "wordbank/daily-challenges-es.json",
            GameLanguage.ENGLISH, "wordbank/daily-challenges-en.json");

    private final Map<GameLanguage, List<DailyEntry>> calendarByLanguage;

    public CuratedDailyChainWordBank(ObjectMapper objectMapper) {
        this(RESOURCE_BY_LANGUAGE.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> loadCalendar(objectMapper, entry.getValue()))));
    }

    /** Package-visible for tests: lets a fixed calendar be injected directly. */
    CuratedDailyChainWordBank(Map<GameLanguage, List<DailyEntry>> calendarByLanguage) {
        calendarByLanguage.forEach((language, calendar) -> {
            if (calendar.isEmpty()) {
                throw new IllegalStateException("daily challenge calendar for " + language + " must not be empty");
            }
        });
        this.calendarByLanguage = calendarByLanguage;
    }

    private static List<DailyEntry> loadCalendar(ObjectMapper objectMapper, String resourcePath) {
        try (InputStream inputStream = new ClassPathResource(resourcePath).getInputStream()) {
            return objectMapper.readValue(inputStream, new TypeReference<List<DailyEntry>>() {
            });
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load daily challenge calendar from " + resourcePath, e);
        }
    }

    /**
     * Picks today's (UTC) entry from the curated calendar rather than
     * drawing randomly, so every player who opens the daily challenge on the
     * same UTC date gets the identical hand-checked phases. {@code seed} is
     * accepted only to satisfy {@link ChainWordBank#fullChain(GameLanguage,
     * int, long)}'s signature — the actual selection is the current date,
     * not the seed, since the whole point of a curated calendar is that it
     * doesn't need pseudo-randomness to be unpredictable.
     */
    @Override
    public List<WordSet> fullChain(GameLanguage language, int phaseCount, long seed) {
        return todaysChain(language);
    }

    List<WordSet> todaysChain(GameLanguage language) {
        List<DailyEntry> calendar = calendarByLanguage.get(language);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        int dayOfYear = today.getDayOfYear();
        DailyEntry entry = calendar.get((dayOfYear - 1) % calendar.size());
        List<String> words = entry.words();
        if (words.size() < 2) {
            throw new IllegalStateException("daily entry for day " + entry.day() + " needs at least 2 words");
        }
        return IntStream.range(0, words.size() - 1)
                .mapToObj(i -> new WordSet(words.get(i), words.get(i + 1), unusedInfiltratorTarget(words.get(i + 1), i)))
                .toList();
    }

    /**
     * WordSet requires a non-blank infiltratorTargetWord distinct from
     * groupTargetWord, but a solo daily room never has an infiltrator (see
     * class javadoc) so this value is never shown to, or reachable by, the
     * player — Round.reachesInfiltratorTarget only fires for a submission
     * from a non-infiltrator player, and GameApplicationService checks that
     * before reachesGroupTarget, so if this were ever equal to (or,
     * accidentally, typeable as) the real target word, correctly finishing
     * a phase would be misread as losing to an "infiltrator" that doesn't
     * exist. Building it around a newline — which no player submission can
     * ever contain, since chain submissions are single trimmed words or
     * short phrases — keeps it structurally unreachable without needing a
     * whole separate curated word per phase purely to sit unused.
     */
    private static String unusedInfiltratorTarget(String groupTargetWord, int phaseIndex) {
        return "unused-infiltrator-target\n" + phaseIndex + "\n" + groupTargetWord;
    }

    @Override
    public WordSet firstWordSet(GameLanguage language) {
        return todaysChain(language).get(0);
    }

    @Override
    public WordSet nextPhaseWordSet(GameLanguage language, String startWord, Set<String> usedWords) {
        List<WordSet> chain = todaysChain(language);
        for (WordSet wordSet : chain) {
            if (wordSet.startWord().equalsIgnoreCase(startWord)) {
                return wordSet;
            }
        }
        throw new IllegalStateException(
                "no curated daily phase starts with \"" + startWord + "\" for " + language);
    }

    record DailyEntry(int day, List<String> words) {
    }
}
