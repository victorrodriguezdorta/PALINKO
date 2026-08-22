package com.kawser.cleanspringbootproject.leaderboard.application.service;

import com.kawser.cleanspringbootproject.leaderboard.application.dto.DailyLeaderboardRow;
import com.kawser.cleanspringbootproject.leaderboard.application.port.in.GetDailyLeaderboardUseCase;
import com.kawser.cleanspringbootproject.leaderboard.application.port.out.DailyLeaderboardRepository;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Orchestrates the leaderboard module's one use case — mirrors
 * GameApplicationService/UserApplicationService's shape even though there's
 * only a single in-port so far.
 */
public class LeaderboardApplicationService implements GetDailyLeaderboardUseCase {

    private static final int TOP_SIZE = 10;

    private final DailyLeaderboardRepository dailyLeaderboardRepository;

    public LeaderboardApplicationService(DailyLeaderboardRepository dailyLeaderboardRepository) {
        this.dailyLeaderboardRepository = dailyLeaderboardRepository;
    }

    @Override
    public List<DailyLeaderboardRow> getToday() {
        // UTC calendar day, same convention as DailySeed.today() and
        // UserApplicationService.recordDailyCompletion.
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        return dailyLeaderboardRepository.topForDate(today, TOP_SIZE);
    }
}
