package com.kawser.cleanspringbootproject.secretmode.application.port.in;

import com.kawser.cleanspringbootproject.secretmode.application.dto.CreateSecretDailyRoomCommand;
import com.kawser.cleanspringbootproject.secretmode.application.dto.CreateSecretRoomResult;

public interface CreateSecretDailyRoomUseCase {

    CreateSecretRoomResult createSecretDailyRoom(CreateSecretDailyRoomCommand command);
}
