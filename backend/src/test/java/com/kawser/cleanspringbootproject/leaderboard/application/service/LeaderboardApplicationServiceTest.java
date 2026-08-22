package com.kawser.cleanspringbootproject.leaderboard.application.service;

import com.kawser.cleanspringbootproject.leaderboard.application.dto.DailyLeaderboardRow;
import com.kawser.cleanspringbootproject.leaderboard.application.port.out.DailyLeaderboardRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LeaderboardApplicationServiceTest {

    @Test
    void getTodayAsksTheRepositoryForTodaysTopTenInUtc() {
        DailyLeaderboardRepository repository = mock(DailyLeaderboardRepository.class);
        List<DailyLeaderboardRow> expected = List.of(
                new DailyLeaderboardRow(1, 1L, "alice", "seed-alice", 5, 90, 3),
                new DailyLeaderboardRow(2, 2L, "bob", "seed-bob", 6, 95, 0));
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        when(repository.topForDate(eq(today), eq(10))).thenReturn(expected);
        LeaderboardApplicationService service = new LeaderboardApplicationService(repository);

        List<DailyLeaderboardRow> result = service.getToday();

        assertThat(result).isEqualTo(expected);
        verify(repository).topForDate(today, 10);
    }

    @Test
    void getTodayPassesThroughAnEmptyResultUnchanged() {
        DailyLeaderboardRepository repository = mock(DailyLeaderboardRepository.class);
        when(repository.topForDate(any(LocalDate.class), anyInt())).thenReturn(List.of());
        LeaderboardApplicationService service = new LeaderboardApplicationService(repository);

        assertThat(service.getToday()).isEmpty();
    }
}
