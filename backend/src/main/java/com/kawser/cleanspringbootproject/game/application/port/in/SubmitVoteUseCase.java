package com.kawser.cleanspringbootproject.game.application.port.in;

import com.kawser.cleanspringbootproject.game.application.dto.SubmitVoteCommand;

public interface SubmitVoteUseCase {

    void submitVote(SubmitVoteCommand command);
}
