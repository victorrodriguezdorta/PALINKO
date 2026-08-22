package com.kawser.cleanspringbootproject.game.application.port.out;

/**
 * Lets the game module report a completed daily challenge's result without
 * depending on the user/leaderboard modules directly — implemented outside
 * this module's own package tree, mirroring DailyStreakTracker.
 */
public interface DailyLeaderboardRecorder {

    void recordResult(String username, int wordCount, int accuracyPercent);
}
