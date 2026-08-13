package com.kawser.cleanspringbootproject.secretmode.application.port.in;

import com.kawser.cleanspringbootproject.secretmode.application.dto.SubmitSolutionCommand;

public interface SubmitSolutionUseCase {

    void submitSolution(SubmitSolutionCommand command);
}
