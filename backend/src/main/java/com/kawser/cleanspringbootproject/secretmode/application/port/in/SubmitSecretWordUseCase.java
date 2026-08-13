package com.kawser.cleanspringbootproject.secretmode.application.port.in;

import com.kawser.cleanspringbootproject.secretmode.application.dto.SubmitSecretWordCommand;

public interface SubmitSecretWordUseCase {

    void submitWord(SubmitSecretWordCommand command);
}
