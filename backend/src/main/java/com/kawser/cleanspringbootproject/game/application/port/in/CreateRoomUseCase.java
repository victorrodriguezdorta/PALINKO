package com.kawser.cleanspringbootproject.game.application.port.in;

import com.kawser.cleanspringbootproject.game.application.dto.CreateRoomCommand;
import com.kawser.cleanspringbootproject.game.application.dto.CreateRoomResult;

public interface CreateRoomUseCase {

    CreateRoomResult createRoom(CreateRoomCommand command);
}
