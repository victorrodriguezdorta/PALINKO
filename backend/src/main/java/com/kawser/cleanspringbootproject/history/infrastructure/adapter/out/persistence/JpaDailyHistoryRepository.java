package com.kawser.cleanspringbootproject.history.infrastructure.adapter.out.persistence;

import com.kawser.cleanspringbootproject.history.application.port.out.DailyHistoryRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public class JpaDailyHistoryRepository implements DailyHistoryRepository {

    private final HistoryJpaRepository jpaRepository;

    public JpaDailyHistoryRepository(HistoryJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void upsertLatest(Long userId, String username, LocalDate challengeDate, String wordChain) {
        HistoryJpaEntity entity = jpaRepository.findByUserIdAndChallengeDate(userId, challengeDate)
                .orElseGet(() -> new HistoryJpaEntity(userId, username, challengeDate, wordChain));
        entity.setUsername(username);
        entity.setWordChain(wordChain);
        jpaRepository.save(entity);
    }

    @Override
    public List<LocalDate> findCompletedDates(Long userId) {
        return jpaRepository.findCompletedDatesByUserId(userId);
    }
}
