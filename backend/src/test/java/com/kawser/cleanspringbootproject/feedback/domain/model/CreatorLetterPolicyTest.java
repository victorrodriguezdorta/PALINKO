package com.kawser.cleanspringbootproject.feedback.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CreatorLetterPolicyTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 26);

    @Test
    void hiddenUntilMoreThanFiveDailyGames() {
        assertThat(CreatorLetterPolicy.shouldShow(5, Optional.empty(), TODAY)).isFalse();
        assertThat(CreatorLetterPolicy.shouldShow(6, Optional.empty(), TODAY)).isTrue();
    }

    @Test
    void neverShownAgainOnceAnswered() {
        var answered = new NotificationRecord(1L, NotificationTopic.CREATOR_LETTER, NotificationStatus.ANSWERED, null);

        assertThat(CreatorLetterPolicy.shouldShow(50, Optional.of(answered), TODAY)).isFalse();
    }

    @Test
    void dismissalHidesItOnlyForTheRestOfThatDay() {
        var dismissedToday = new NotificationRecord(
                1L, NotificationTopic.CREATOR_LETTER, NotificationStatus.DISMISSED, TODAY);

        assertThat(CreatorLetterPolicy.shouldShow(6, Optional.of(dismissedToday), TODAY)).isFalse();
        assertThat(CreatorLetterPolicy.shouldShow(6, Optional.of(dismissedToday), TODAY.plusDays(1))).isTrue();
    }
}
