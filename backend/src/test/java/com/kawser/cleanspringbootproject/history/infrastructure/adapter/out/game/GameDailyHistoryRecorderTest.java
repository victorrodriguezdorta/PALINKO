package com.kawser.cleanspringbootproject.history.infrastructure.adapter.out.game;

import com.kawser.cleanspringbootproject.history.application.port.out.DailyHistoryRepository;
import com.kawser.cleanspringbootproject.user.application.port.out.UserRepository;
import com.kawser.cleanspringbootproject.user.domain.model.User;
import com.kawser.cleanspringbootproject.user.domain.model.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameDailyHistoryRecorderTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DailyHistoryRepository dailyHistoryRepository;

    @InjectMocks
    private GameDailyHistoryRecorder recorder;

    @Test
    void incrementsDailyGamesPlayedForEachCompletedChallengeOnTheSameDate() {
        LocalDate challengeDate = LocalDate.of(2026, 11, 26);
        User user = new User(
                7L, "player", "hash", "player@example.com", true, UserRole.USER,
                "avatar", null, 0, null, 0);
        when(userRepository.findByUsername("player")).thenReturn(Optional.of(user));

        recorder.recordSolution("player", challengeDate, "one-two");
        recorder.recordSolution("player", challengeDate, "three-four");

        verify(userRepository, times(2)).incrementDailyGamesPlayed(7L);
        verify(dailyHistoryRepository).upsertLatest(7L, "player", challengeDate, "one-two");
        verify(dailyHistoryRepository).upsertLatest(7L, "player", challengeDate, "three-four");
    }
}
