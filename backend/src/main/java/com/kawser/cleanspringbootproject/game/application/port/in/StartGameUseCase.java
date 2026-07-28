package com.kawser.cleanspringbootproject.game.application.port.in;

import com.kawser.cleanspringbootproject.game.application.dto.StartGameCommand;

public interface StartGameUseCase {

    void startGame(StartGameCommand command);
}
