package com.kawser.cleanspringbootproject.secretmode.application.port.in;

import com.kawser.cleanspringbootproject.secretmode.application.dto.ReconnectSecretPlayerCommand;
import com.kawser.cleanspringbootproject.secretmode.application.dto.SecretRoomSnapshot;

public interface ReconnectSecretPlayerUseCase {

    SecretRoomSnapshot reconnect(ReconnectSecretPlayerCommand command);
}
