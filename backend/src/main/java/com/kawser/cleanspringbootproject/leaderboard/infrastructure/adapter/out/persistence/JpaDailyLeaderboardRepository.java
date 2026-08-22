package com.kawser.cleanspringbootproject.leaderboard.infrastructure.adapter.out.persistence;

import com.kawser.cleanspringbootproject.leaderboard.application.dto.DailyLeaderboardRow;
import com.kawser.cleanspringbootproject.leaderboard.application.port.out.DailyLeaderboardRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class JpaDailyLeaderboardRepository implements DailyLeaderboardRepository {

    private final DailyLeaderboardEntryJpaRepository jpaRepository;

    public JpaDailyLeaderboardRepository(DailyLeaderboardEntryJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    /**
     * Fewer words wins; a tie is broken by higher accuracy. A brand new
     * result for the day is always inserted; an existing one is only
     * overwritten if the new attempt is strictly better by that ordering,
     * so a worse replay never displaces a better earlier one.
     */
    @Override
    public void upsertIfBetter(
            Long userId, LocalDate challengeDate, int wordCount, int accuracyPercent, Instant completedAt) {
        Optional<DailyLeaderboardEntryJpaEntity> existing =
                jpaRepository.findByUserIdAndChallengeDate(userId, challengeDate);
        if (existing.isEmpty()) {
            jpaRepository.save(
                    new DailyLeaderboardEntryJpaEntity(null, userId, challengeDate, wordCount, accuracyPercent, completedAt));
            return;
        }

        DailyLeaderboardEntryJpaEntity entity = existing.get();
        boolean better = wordCount < entity.getWordCount()
                || (wordCount == entity.getWordCount() && accuracyPercent > entity.getAccuracyPercent());
        if (!better) {
            return;
        }
        entity.setWordCount(wordCount);
        entity.setAccuracyPercent(accuracyPercent);
        entity.setCompletedAt(completedAt);
        jpaRepository.save(entity);
    }

    @Override
    public List<DailyLeaderboardRow> topForDate(LocalDate challengeDate, int limit) {
        List<DailyLeaderboardRowProjection> rows =
                jpaRepository.findTopForDate(challengeDate, PageRequest.of(0, limit));
        return java.util.stream.IntStream.range(0, rows.size())
                .mapToObj(i -> {
                    DailyLeaderboardRowProjection row = rows.get(i);
                    return new DailyLeaderboardRow(
                            i + 1, row.getUserId(), row.getUsername(), row.getAvatarSeed(),
                            row.getWordCount(), row.getAccuracyPercent(), row.getCurrentStreak());
                })
                .toList();
    }
}
