package com.kawser.cleanspringbootproject.game.infrastructure.adapter.out.spelling;

import com.kawser.cleanspringbootproject.game.domain.model.GameLanguage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HunspellWordSpellingCorrectorTest {

    private final HunspellWordSpellingCorrector corrector = new HunspellWordSpellingCorrector();

    @Test
    void fixesAClearOneLetterTypo() {
        assertThat(corrector.correct("porfesor", GameLanguage.SPANISH)).isEqualTo("profesor");
    }

    @Test
    void leavesAWordNotInTheDictionaryUnchangedRatherThanJumpingToAnUnrelatedWord() {
        // "koala" isn't in the es_ES Hunspell dictionary, whose top
        // suggestion for it is the unrelated word "toalla" (distance 3+ on
        // a 5-letter word). The edit-distance gate must reject that jump.
        assertThat(corrector.correct("koala", GameLanguage.SPANISH)).isEqualTo("koala");
    }

    @Test
    void leavesACorrectlySpelledWordUnchanged() {
        assertThat(corrector.correct("gato", GameLanguage.SPANISH)).isEqualTo("gato");
    }

    @Test
    void correctsEachTokenOfATwoWordPhraseIndependently() {
        assertThat(corrector.correct("buenos días", GameLanguage.SPANISH)).isEqualTo("buenos días");
    }
}
