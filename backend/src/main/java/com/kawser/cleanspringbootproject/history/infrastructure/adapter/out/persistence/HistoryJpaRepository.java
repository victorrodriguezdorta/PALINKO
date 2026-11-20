package com.kawser.cleanspringbootproject.history.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HistoryJpaRepository extends JpaRepository<HistoryJpaEntity, Long> {

    Optional<HistoryJpaEntity> findByUserIdAndChallengeDate(Long userId, LocalDate challengeDate);

    @Query("select h.challengeDate from HistoryJpaEntity h "
            + "where h.userId = :userId order by h.challengeDate")
    List<LocalDate> findCompletedDatesByUserId(@Param("userId") Long userId);
}
