package com.kawser.cleanspringbootproject.leaderboard.application.dto;

public record DailyLeaderboardRow(
        int rank,
        Long userId,
        String username,
        String avatarSeed,
        int wordCount,
        int accuracyPercent,
        int currentStreak) {
}
