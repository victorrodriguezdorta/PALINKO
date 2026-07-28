package com.kawser.cleanspringbootproject.game.application.port.in;

import com.kawser.cleanspringbootproject.game.application.dto.SubmitAnswerCommand;

public interface SubmitAnswerUseCase {

    void submitAnswer(SubmitAnswerCommand command);
}
