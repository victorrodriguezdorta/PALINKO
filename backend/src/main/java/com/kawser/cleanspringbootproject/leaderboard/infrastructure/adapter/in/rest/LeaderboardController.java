package com.kawser.cleanspringbootproject.leaderboard.infrastructure.adapter.in.rest;

import com.kawser.cleanspringbootproject.leaderboard.application.port.in.GetDailyLeaderboardUseCase;
import com.kawser.cleanspringbootproject.leaderboard.infrastructure.adapter.in.rest.dto.DailyLeaderboardEntryResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class LeaderboardController {

    private final GetDailyLeaderboardUseCase getDailyLeaderboardUseCase;

    public LeaderboardController(GetDailyLeaderboardUseCase getDailyLeaderboardUseCase) {
        this.getDailyLeaderboardUseCase = getDailyLeaderboardUseCase;
    }

    @GetMapping("/leaderboard/daily")
    public ResponseEntity<List<DailyLeaderboardEntryResponse>> getDailyLeaderboard() {
        List<DailyLeaderboardEntryResponse> response = getDailyLeaderboardUseCase.getToday().stream()
                .map(DailyLeaderboardEntryResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }
}
