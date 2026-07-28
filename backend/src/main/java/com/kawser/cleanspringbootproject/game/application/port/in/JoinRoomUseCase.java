package com.kawser.cleanspringbootproject.game.application.port.in;

import com.kawser.cleanspringbootproject.game.application.dto.JoinRoomCommand;
import com.kawser.cleanspringbootproject.game.application.dto.JoinRoomResult;

public interface JoinRoomUseCase {

    JoinRoomResult joinRoom(JoinRoomCommand command);
}
