package com.kawser.cleanspringbootproject.leaderboard.infrastructure.adapter.in.rest.dto;

import com.kawser.cleanspringbootproject.leaderboard.application.dto.DailyLeaderboardRow;

public record DailyLeaderboardEntryResponse(
        int rank,
        Long userId,
        String username,
        String avatarSeed,
        int wordCount,
        int accuracyPercent,
        int currentStreak) {

    public static DailyLeaderboardEntryResponse from(DailyLeaderboardRow row) {
        return new DailyLeaderboardEntryResponse(
                row.rank(), row.userId(), row.username(), row.avatarSeed(), row.wordCount(), row.accuracyPercent(),
                row.currentStreak());
    }
}
