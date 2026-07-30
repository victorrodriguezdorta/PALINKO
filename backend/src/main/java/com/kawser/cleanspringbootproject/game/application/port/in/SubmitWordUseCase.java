package com.kawser.cleanspringbootproject.game.application.port.in;

import com.kawser.cleanspringbootproject.game.application.dto.SubmitWordCommand;

public interface SubmitWordUseCase {

    void submitWord(SubmitWordCommand command);
}
