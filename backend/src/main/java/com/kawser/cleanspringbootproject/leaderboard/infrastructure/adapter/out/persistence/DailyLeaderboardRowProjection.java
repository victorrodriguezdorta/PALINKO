package com.kawser.cleanspringbootproject.leaderboard.infrastructure.adapter.out.persistence;

/**
 * JPQL constructor-expression target for the ranked top-N query — a plain
 * class (not a record) because Hibernate's "new" query syntax requires a
 * matching public constructor, which records already provide, but the
 * fields are still exposed via getters for JpaDailyLeaderboardRepository to
 * map into the application-level DailyLeaderboardRow (which additionally
 * carries the 1-based rank the query itself doesn't compute).
 */
public class DailyLeaderboardRowProjection {

    private final Long userId;
    private final String username;
    private final String avatarSeed;
    private final int wordCount;
    private final int accuracyPercent;
    private final int currentStreak;

    public DailyLeaderboardRowProjection(
            Long userId, String username, String avatarSeed, int wordCount, int accuracyPercent, int currentStreak) {
        this.userId = userId;
        this.username = username;
        this.avatarSeed = avatarSeed;
        this.wordCount = wordCount;
        this.accuracyPercent = accuracyPercent;
        this.currentStreak = currentStreak;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getAvatarSeed() {
        return avatarSeed;
    }

    public int getWordCount() {
        return wordCount;
    }

    public int getAccuracyPercent() {
        return accuracyPercent;
    }

    public int getCurrentStreak() {
        return currentStreak;
    }
}
