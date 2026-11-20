package com.kawser.cleanspringbootproject.history.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface HistoryJpaRepository extends JpaRepository<HistoryJpaEntity, Long> {

    Optional<HistoryJpaEntity> findByUserIdAndChallengeDate(Long userId, LocalDate challengeDate);
}
