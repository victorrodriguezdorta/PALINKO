package com.kawser.cleanspringbootproject.secretmode.application.port.in;

import com.kawser.cleanspringbootproject.secretmode.application.dto.GiveUpCommand;

public interface GiveUpUseCase {

    void giveUp(GiveUpCommand command);
}
