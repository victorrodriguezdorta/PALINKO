package com.kawser.cleanspringbootproject.history.infrastructure.adapter.in.rest;

import com.kawser.cleanspringbootproject.history.application.port.in.GetCompletedDailyChallengesUseCase;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/users/me/daily-history")
public class DailyHistoryController {

    private final GetCompletedDailyChallengesUseCase getCompletedDailyChallengesUseCase;

    public DailyHistoryController(GetCompletedDailyChallengesUseCase getCompletedDailyChallengesUseCase) {
        this.getCompletedDailyChallengesUseCase = getCompletedDailyChallengesUseCase;
    }

    @GetMapping
    public List<LocalDate> getCompletedDates(Authentication authentication) {
        return getCompletedDailyChallengesUseCase.getCompletedDates(authentication.getName());
    }
}
