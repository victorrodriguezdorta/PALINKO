package com.kawser.cleanspringbootproject.leaderboard.application.port.out;

import com.kawser.cleanspringbootproject.leaderboard.application.dto.DailyLeaderboardRow;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public interface DailyLeaderboardRepository {

    /**
     * Records a completed daily run, keeping only the best of the day per
     * user: fewer accepted words wins, ties broken by higher accuracy. A
     * worse (or equal) retry than what's already stored is silently
     * ignored rather than overwriting it.
     */
    void upsertIfBetter(Long userId, LocalDate challengeDate, int wordCount, int accuracyPercent, Instant completedAt);

    /** Top entries for the given day, ranked fewest words first, ties broken by higher accuracy. */
    List<DailyLeaderboardRow> topForDate(LocalDate challengeDate, int limit);
}
