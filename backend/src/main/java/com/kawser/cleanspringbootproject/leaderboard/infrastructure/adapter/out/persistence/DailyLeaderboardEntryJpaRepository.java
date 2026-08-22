package com.kawser.cleanspringbootproject.leaderboard.infrastructure.adapter.out.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyLeaderboardEntryJpaRepository extends JpaRepository<DailyLeaderboardEntryJpaEntity, Long> {

    Optional<DailyLeaderboardEntryJpaEntity> findByUserIdAndChallengeDate(Long userId, LocalDate challengeDate);

    @Modifying
    @Transactional
    void deleteByChallengeDateNot(LocalDate challengeDate);

    @Query("""
            select new com.kawser.cleanspringbootproject.leaderboard.infrastructure.adapter.out.persistence.DailyLeaderboardRowProjection(
                e.userId, u.username, u.avatarSeed, e.wordCount, e.accuracyPercent, u.currentStreak)
            from DailyLeaderboardEntryJpaEntity e
            join UserJpaEntity u on u.id = e.userId
            where e.challengeDate = :challengeDate
            order by e.wordCount asc, e.accuracyPercent desc, e.id asc
            """)
    List<DailyLeaderboardRowProjection> findTopForDate(LocalDate challengeDate, Pageable pageable);
}
