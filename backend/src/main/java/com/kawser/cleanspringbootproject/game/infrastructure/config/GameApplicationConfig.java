package com.kawser.cleanspringbootproject.game.infrastructure.config;

import com.kawser.cleanspringbootproject.game.application.port.out.AiAnswerGenerator;
import com.kawser.cleanspringbootproject.game.application.port.out.PhaseScheduler;
import com.kawser.cleanspringbootproject.game.application.port.out.QuestionBank;
import com.kawser.cleanspringbootproject.game.application.port.out.RoomCodeGenerator;
import com.kawser.cleanspringbootproject.game.application.port.out.RoomNotifier;
import com.kawser.cleanspringbootproject.game.application.port.out.RoomRepository;
import com.kawser.cleanspringbootproject.game.application.service.GameApplicationService;
import com.kawser.cleanspringbootproject.game.domain.service.DefaultScoringPolicy;
import com.kawser.cleanspringbootproject.game.domain.service.ScoringPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the framework-agnostic GameApplicationService (which implements
 * every application.port.in use case) to its out-port adapters. Kept
 * separate from the adapters themselves so the application layer never has
 * an annotation of its own.
 */
@Configuration
public class GameApplicationConfig {

    @Bean
    public ScoringPolicy scoringPolicy() {
        return new DefaultScoringPolicy();
    }

    @Bean
    public GameApplicationService gameApplicationService(
            RoomRepository roomRepository,
            RoomNotifier roomNotifier,
            AiAnswerGenerator aiAnswerGenerator,
            QuestionBank questionBank,
            RoomCodeGenerator roomCodeGenerator,
            PhaseScheduler phaseScheduler,
            ScoringPolicy scoringPolicy) {
        return new GameApplicationService(
                roomRepository, roomNotifier, aiAnswerGenerator, questionBank,
                roomCodeGenerator, phaseScheduler, scoringPolicy);
    }
}
