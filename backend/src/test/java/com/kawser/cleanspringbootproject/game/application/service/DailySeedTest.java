package com.kawser.cleanspringbootproject.game.application.service;

import com.kawser.cleanspringbootproject.game.domain.model.GameLanguage;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class DailySeedTest {

    @Test
    void sameDateAndLanguageAlwaysProduceTheSameSeed() {
        LocalDate date = LocalDate.of(2026, 7, 30);

        long first = DailySeed.seedFor(date, GameLanguage.ENGLISH);
        long second = DailySeed.seedFor(date, GameLanguage.ENGLISH);

        assertThat(first).isEqualTo(second);
    }

    @Test
    void differentDatesProduceDifferentSeeds() {
        long today = DailySeed.seedFor(LocalDate.of(2026, 7, 30), GameLanguage.ENGLISH);
        long tomorrow = DailySeed.seedFor(LocalDate.of(2026, 7, 31), GameLanguage.ENGLISH);

        assertThat(today).isNotEqualTo(tomorrow);
    }

    @Test
    void differentLanguagesOnTheSameDateProduceDifferentSeeds() {
        LocalDate date = LocalDate.of(2026, 7, 30);

        long english = DailySeed.seedFor(date, GameLanguage.ENGLISH);
        long spanish = DailySeed.seedFor(date, GameLanguage.SPANISH);

        assertThat(english).isNotEqualTo(spanish);
    }
}
