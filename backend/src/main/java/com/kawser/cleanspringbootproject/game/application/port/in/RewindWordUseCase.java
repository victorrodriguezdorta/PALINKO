package com.kawser.cleanspringbootproject.game.application.port.in;

import com.kawser.cleanspringbootproject.game.application.dto.RewindWordCommand;

/**
 * Current-turn-only, once per player per game: undoes the most recently
 * accepted word in the chain (see Round.rewindLastAcceptedWord), handing
 * the relatedness-to-previous target back to whatever was accepted before
 * it and reversing the score that word's author earned for it.
 */
public interface RewindWordUseCase {

    void rewindWord(RewindWordCommand command);
}
