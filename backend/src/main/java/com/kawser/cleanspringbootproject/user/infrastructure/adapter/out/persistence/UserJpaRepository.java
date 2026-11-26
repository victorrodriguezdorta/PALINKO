package com.kawser.cleanspringbootproject.user.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, Long> {

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Optional<UserJpaEntity> findByUsername(String username);

    Optional<UserJpaEntity> findByEmail(String email);

    List<UserJpaEntity> findByBotTrue();

    @Modifying
    @Transactional
    @Query("update UserJpaEntity user set user.dailyGamesPlayed = user.dailyGamesPlayed + 1 where user.id = :userId")
    void incrementDailyGamesPlayed(@Param("userId") Long userId);
}
