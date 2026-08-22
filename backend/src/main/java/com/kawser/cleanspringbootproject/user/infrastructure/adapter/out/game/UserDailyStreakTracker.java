package com.kawser.cleanspringbootproject.user.infrastructure.adapter.out.game;

import com.kawser.cleanspringbootproject.game.application.port.out.DailyStreakTracker;
import com.kawser.cleanspringbootproject.user.application.port.in.RecordDailyCompletionUseCase;
import org.springframework.stereotype.Component;

/**
 * Adapts the game module's DailyStreakTracker out-port to this module's own
 * RecordDailyCompletionUseCase — keeps game.application from depending on
 * anything under the user package directly.
 */
@Component
public class UserDailyStreakTracker implements DailyStreakTracker {

    private final RecordDailyCompletionUseCase recordDailyCompletionUseCase;

    public UserDailyStreakTracker(RecordDailyCompletionUseCase recordDailyCompletionUseCase) {
        this.recordDailyCompletionUseCase = recordDailyCompletionUseCase;
    }

    @Override
    public void recordDailyCompletion(String username) {
        recordDailyCompletionUseCase.recordDailyCompletion(username);
    }
}
