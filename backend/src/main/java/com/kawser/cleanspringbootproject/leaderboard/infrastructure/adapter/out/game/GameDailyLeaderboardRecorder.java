package com.kawser.cleanspringbootproject.leaderboard.infrastructure.adapter.out.game;

import com.kawser.cleanspringbootproject.game.application.port.out.DailyLeaderboardRecorder;
import com.kawser.cleanspringbootproject.leaderboard.application.port.out.DailyLeaderboardRepository;
import com.kawser.cleanspringbootproject.user.application.port.out.UserRepository;
import com.kawser.cleanspringbootproject.user.domain.model.User;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Adapts the game module's DailyLeaderboardRecorder out-port to this
 * module's own persistence port — keeps game.application from depending on
 * anything under leaderboard/user directly, mirroring
 * user.infrastructure.adapter.out.game.UserDailyStreakTracker.
 */
@Component
public class GameDailyLeaderboardRecorder implements DailyLeaderboardRecorder {

    private final UserRepository userRepository;
    private final DailyLeaderboardRepository dailyLeaderboardRepository;

    public GameDailyLeaderboardRecorder(UserRepository userRepository, DailyLeaderboardRepository dailyLeaderboardRepository) {
        this.userRepository = userRepository;
        this.dailyLeaderboardRepository = dailyLeaderboardRepository;
    }

    @Override
    public void recordResult(String username, int wordCount, int accuracyPercent) {
        // Silently ignored if the account is missing, same rationale as
        // UserApplicationService.recordDailyCompletion: the caller is a
        // just-finished daily room with no reasonable way to surface an
        // error back to the player after the game already ended.
        userRepository.findByUsername(username).map(User::id).ifPresent(userId -> {
            LocalDate today = LocalDate.now(ZoneOffset.UTC);
            dailyLeaderboardRepository.upsertIfBetter(userId, today, wordCount, accuracyPercent, Instant.now());
        });
    }
}
