package com.kawser.cleanspringbootproject.game.application.port.out;

import java.time.LocalDate;

/**
 * Stores the final accepted word chain for a completed daily challenge.
 */
public interface DailyHistoryRecorder {

    void recordSolution(String username, LocalDate challengeDate, String wordChain);
}
