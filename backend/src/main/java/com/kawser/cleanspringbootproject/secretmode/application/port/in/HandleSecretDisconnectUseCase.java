package com.kawser.cleanspringbootproject.secretmode.application.port.in;

import com.kawser.cleanspringbootproject.secretmode.application.dto.DisconnectSecretPlayerCommand;

public interface HandleSecretDisconnectUseCase {

    void handleDisconnect(DisconnectSecretPlayerCommand command);
}
