package com.kawser.cleanspringbootproject.game.application.port.in;

import com.kawser.cleanspringbootproject.game.application.dto.CreateDailyRoomCommand;
import com.kawser.cleanspringbootproject.game.application.dto.CreateRoomResult;

public interface CreateDailyRoomUseCase {

    CreateRoomResult createDailyRoom(CreateDailyRoomCommand command);
}
