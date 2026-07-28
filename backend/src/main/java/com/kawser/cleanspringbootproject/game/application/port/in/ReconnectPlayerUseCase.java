package com.kawser.cleanspringbootproject.game.application.port.in;

import com.kawser.cleanspringbootproject.game.application.dto.ReconnectCommand;
import com.kawser.cleanspringbootproject.game.application.dto.RoomSnapshot;

public interface ReconnectPlayerUseCase {

    RoomSnapshot reconnect(ReconnectCommand command);
}
