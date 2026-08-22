package com.kawser.cleanspringbootproject.leaderboard.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "daily_leaderboard_entries")
public class DailyLeaderboardEntryJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "challenge_date", nullable = false)
    private LocalDate challengeDate;

    @Column(name = "word_count", nullable = false)
    private int wordCount;

    @Column(name = "accuracy_percent", nullable = false)
    private int accuracyPercent;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    protected DailyLeaderboardEntryJpaEntity() {
    }

    public DailyLeaderboardEntryJpaEntity(
            Long id, Long userId, LocalDate challengeDate, int wordCount, int accuracyPercent, Instant completedAt) {
        this.id = id;
        this.userId = userId;
        this.challengeDate = challengeDate;
        this.wordCount = wordCount;
        this.accuracyPercent = accuracyPercent;
        this.completedAt = completedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public LocalDate getChallengeDate() {
        return challengeDate;
    }

    public int getWordCount() {
        return wordCount;
    }

    public void setWordCount(int wordCount) {
        this.wordCount = wordCount;
    }

    public int getAccuracyPercent() {
        return accuracyPercent;
    }

    public void setAccuracyPercent(int accuracyPercent) {
        this.accuracyPercent = accuracyPercent;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
