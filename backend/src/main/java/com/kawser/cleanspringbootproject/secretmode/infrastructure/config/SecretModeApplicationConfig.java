package com.kawser.cleanspringbootproject.secretmode.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kawser.cleanspringbootproject.game.application.port.out.WordSpellingCorrector;
import com.kawser.cleanspringbootproject.game.application.port.out.WordSubmissionRateLimiter;
import com.kawser.cleanspringbootproject.secretmode.application.port.out.ConceptBank;
import com.kawser.cleanspringbootproject.secretmode.application.port.out.SecretQualityChecker;
import com.kawser.cleanspringbootproject.secretmode.application.port.out.SecretRoomCodeGenerator;
import com.kawser.cleanspringbootproject.secretmode.application.port.out.SecretRoomNotifier;
import com.kawser.cleanspringbootproject.secretmode.application.port.out.SecretRoomRepository;
import com.kawser.cleanspringbootproject.secretmode.application.service.SecretGameApplicationService;
import com.kawser.cleanspringbootproject.secretmode.domain.service.DefaultSecretScoringPolicy;
import com.kawser.cleanspringbootproject.secretmode.domain.service.SecretScoringPolicy;
import com.kawser.cleanspringbootproject.secretmode.infrastructure.adapter.out.wordbank.JsonConceptBank;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the framework-agnostic SecretGameApplicationService to its out-port
 * adapters, mirroring
 * game.infrastructure.config.GameApplicationConfig. WordSpellingCorrector
 * and WordSubmissionRateLimiter are the original game module's beans,
 * reused here as-is (see their javadoc: both are stateless/keyed-by-id with
 * no dependency on Room), so this config only needs to supply
 * secretmode-specific out-ports.
 */
@Configuration
public class SecretModeApplicationConfig {

    @Bean
    public SecretScoringPolicy secretScoringPolicy() {
        return new DefaultSecretScoringPolicy();
    }

    @Bean
    public ConceptBank conceptBank(ObjectMapper objectMapper) {
        return new JsonConceptBank(objectMapper);
    }

    @Bean
    public SecretGameApplicationService secretGameApplicationService(
            SecretRoomRepository secretRoomRepository,
            SecretRoomNotifier secretRoomNotifier,
            SecretQualityChecker secretQualityChecker,
            WordSpellingCorrector wordSpellingCorrector,
            ConceptBank conceptBank,
            SecretRoomCodeGenerator secretRoomCodeGenerator,
            SecretScoringPolicy secretScoringPolicy,
            WordSubmissionRateLimiter wordSubmissionRateLimiter) {
        return new SecretGameApplicationService(
                secretRoomRepository, secretRoomNotifier, secretQualityChecker, wordSpellingCorrector,
                conceptBank, secretRoomCodeGenerator, secretScoringPolicy, wordSubmissionRateLimiter);
    }
}
