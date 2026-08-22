package com.kawser.cleanspringbootproject.leaderboard.infrastructure.config;

import com.kawser.cleanspringbootproject.leaderboard.application.port.out.DailyLeaderboardRepository;
import com.kawser.cleanspringbootproject.leaderboard.application.service.LeaderboardApplicationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the framework-agnostic LeaderboardApplicationService to its
 * out-port adapter — mirrors game.infrastructure.config.GameApplicationConfig
 * and user.infrastructure.config.UserApplicationConfig.
 */
@Configuration
public class LeaderboardApplicationConfig {

    @Bean
    public LeaderboardApplicationService leaderboardApplicationService(
            DailyLeaderboardRepository dailyLeaderboardRepository) {
        return new LeaderboardApplicationService(dailyLeaderboardRepository);
    }
}
