package com.kawser.cleanspringbootproject.history.application.port.in;

import java.time.LocalDate;
import java.util.List;

public interface GetCompletedDailyChallengesUseCase {

    List<LocalDate> getCompletedDates(String username);
}
