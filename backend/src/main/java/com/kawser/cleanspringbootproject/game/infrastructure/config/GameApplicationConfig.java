package com.kawser.cleanspringbootproject.game.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kawser.cleanspringbootproject.game.application.port.out.ChainWordBank;
import com.kawser.cleanspringbootproject.game.application.port.out.PhaseScheduler;
import com.kawser.cleanspringbootproject.game.application.port.out.RoomCodeGenerator;
import com.kawser.cleanspringbootproject.game.application.port.out.RoomNotifier;
import com.kawser.cleanspringbootproject.game.application.port.out.RoomRepository;
import com.kawser.cleanspringbootproject.game.application.port.out.WordRelationChecker;
import com.kawser.cleanspringbootproject.game.application.port.out.WordSpellingCorrector;
import com.kawser.cleanspringbootproject.game.application.port.out.WordSubmissionRateLimiter;
import com.kawser.cleanspringbootproject.game.application.service.GameApplicationService;
import com.kawser.cleanspringbootproject.game.domain.service.DefaultScoringPolicy;
import com.kawser.cleanspringbootproject.game.domain.service.ScoringPolicy;
import com.kawser.cleanspringbootproject.game.infrastructure.adapter.out.wordbank.CuratedDailyChainWordBank;
import com.kawser.cleanspringbootproject.game.infrastructure.adapter.out.wordbank.GroupedChainWordBank;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the framework-agnostic GameApplicationService (which implements
 * every application.port.in use case) to its out-port adapters. Kept
 * separate from the adapters themselves so the application layer never has
 * an annotation of its own.
 *
 * <p>Two {@link ChainWordBank} beans are wired here rather than letting
 * component-scanning pick one implicitly: regular rooms keep drawing random,
 * category-disjoint phases from {@link GroupedChainWordBank}, while the
 * daily challenge draws its phases from {@link CuratedDailyChainWordBank}'s
 * hand-checked calendar instead, since random category separation alone
 * lets through pairs that are secretly easy to bridge (see that class's
 * javadoc) and the daily challenge is meant to be the deliberately hard,
 * same-for-everyone puzzle.
 */
@Configuration
public class GameApplicationConfig {

    @Bean
    public ScoringPolicy scoringPolicy() {
        return new DefaultScoringPolicy();
    }

    @Bean
    public GroupedChainWordBank groupedChainWordBank(ObjectMapper objectMapper) {
        return new GroupedChainWordBank(objectMapper);
    }

    @Bean
    public CuratedDailyChainWordBank curatedDailyChainWordBank(ObjectMapper objectMapper) {
        return new CuratedDailyChainWordBank(objectMapper);
    }

    @Bean
    public GameApplicationService gameApplicationService(
            RoomRepository roomRepository,
            RoomNotifier roomNotifier,
            WordRelationChecker wordRelationChecker,
            WordSpellingCorrector wordSpellingCorrector,
            GroupedChainWordBank groupedChainWordBank,
            CuratedDailyChainWordBank curatedDailyChainWordBank,
            RoomCodeGenerator roomCodeGenerator,
            PhaseScheduler phaseScheduler,
            ScoringPolicy scoringPolicy,
            WordSubmissionRateLimiter wordSubmissionRateLimiter) {
        return new GameApplicationService(
                roomRepository, roomNotifier, wordRelationChecker, wordSpellingCorrector, groupedChainWordBank,
                curatedDailyChainWordBank, roomCodeGenerator, phaseScheduler, scoringPolicy,
                wordSubmissionRateLimiter);
    }
}
