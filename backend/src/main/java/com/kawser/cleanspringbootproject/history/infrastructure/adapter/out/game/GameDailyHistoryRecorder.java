package com.kawser.cleanspringbootproject.history.infrastructure.adapter.out.game;

import com.kawser.cleanspringbootproject.game.application.port.out.DailyHistoryRecorder;
import com.kawser.cleanspringbootproject.history.application.port.out.DailyHistoryRepository;
import com.kawser.cleanspringbootproject.user.application.port.out.UserRepository;
import com.kawser.cleanspringbootproject.user.domain.model.User;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Component
public class GameDailyHistoryRecorder implements DailyHistoryRecorder {

    private final UserRepository userRepository;
    private final DailyHistoryRepository dailyHistoryRepository;

    public GameDailyHistoryRecorder(UserRepository userRepository, DailyHistoryRepository dailyHistoryRepository) {
        this.userRepository = userRepository;
        this.dailyHistoryRepository = dailyHistoryRepository;
    }

    @Override
    @Transactional
    public void recordSolution(String username, LocalDate challengeDate, String wordChain) {
        userRepository.findByUsername(username).ifPresent(user -> {
            userRepository.incrementDailyGamesPlayed(user.id());
            dailyHistoryRepository.upsertLatest(user.id(), user.username(), challengeDate, wordChain);
        });
    }
}
