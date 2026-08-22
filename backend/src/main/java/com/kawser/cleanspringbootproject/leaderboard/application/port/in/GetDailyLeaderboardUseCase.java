package com.kawser.cleanspringbootproject.leaderboard.application.port.in;

import com.kawser.cleanspringbootproject.leaderboard.application.dto.DailyLeaderboardRow;

import java.util.List;

public interface GetDailyLeaderboardUseCase {

    List<DailyLeaderboardRow> getToday();
}
