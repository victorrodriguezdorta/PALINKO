package com.kawser.cleanspringbootproject.game.application.port.in;

import com.kawser.cleanspringbootproject.game.application.dto.DisconnectCommand;

public interface HandleDisconnectUseCase {

    void handleDisconnect(DisconnectCommand command);
}
