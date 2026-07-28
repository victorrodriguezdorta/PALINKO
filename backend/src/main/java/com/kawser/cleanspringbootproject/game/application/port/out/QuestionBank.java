package com.kawser.cleanspringbootproject.game.application.port.out;

import com.kawser.cleanspringbootproject.game.domain.model.Question;

import java.util.Set;

/**
 * Supplies questions for rounds. excludedIds lets a single game avoid
 * repeating a question already used in that same room, without the bank
 * itself keeping any per-room mutable state.
 */
public interface QuestionBank {

    Question nextQuestion(Set<String> excludedIds);
}
