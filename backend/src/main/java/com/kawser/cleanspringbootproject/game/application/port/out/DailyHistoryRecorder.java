package com.kawser.cleanspringbootproject.game.application.port.out;

/**
 * Stores the final accepted word chain for a completed daily challenge.
 */
public interface DailyHistoryRecorder {

    void recordSolution(String username, String wordChain);
}
