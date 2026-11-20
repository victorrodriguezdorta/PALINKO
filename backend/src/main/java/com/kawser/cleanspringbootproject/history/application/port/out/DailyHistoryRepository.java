package com.kawser.cleanspringbootproject.history.application.port.out;

import java.time.LocalDate;

/**
 * Persists one latest daily-challenge solution per user and UTC date.
 */
public interface DailyHistoryRepository {

    void upsertLatest(Long userId, String username, LocalDate challengeDate, String wordChain);
}
