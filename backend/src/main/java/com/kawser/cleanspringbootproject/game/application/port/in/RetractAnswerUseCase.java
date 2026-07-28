package com.kawser.cleanspringbootproject.game.application.port.in;

import com.kawser.cleanspringbootproject.game.application.dto.RetractAnswerCommand;

/**
 * Lets a player cancel their own submitted answer to edit it, as long as
 * ANSWERING is still the active phase.
 */
public interface RetractAnswerUseCase {

    void retractAnswer(RetractAnswerCommand command);
}
