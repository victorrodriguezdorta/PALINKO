package com.kawser.cleanspringbootproject.history.application.service;

import com.kawser.cleanspringbootproject.history.application.port.in.GetCompletedDailyChallengesUseCase;
import com.kawser.cleanspringbootproject.history.application.port.out.DailyHistoryRepository;
import com.kawser.cleanspringbootproject.user.application.port.out.UserRepository;
import com.kawser.cleanspringbootproject.user.domain.exception.UserNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class DailyHistoryApplicationService implements GetCompletedDailyChallengesUseCase {

    private final UserRepository userRepository;
    private final DailyHistoryRepository dailyHistoryRepository;

    public DailyHistoryApplicationService(
            UserRepository userRepository, DailyHistoryRepository dailyHistoryRepository) {
        this.userRepository = userRepository;
        this.dailyHistoryRepository = dailyHistoryRepository;
    }

    @Override
    public List<LocalDate> getCompletedDates(String username) {
        var user = userRepository.findByUsername(username).orElseThrow(UserNotFoundException::new);
        return dailyHistoryRepository.findCompletedDates(user.id());
    }
}
