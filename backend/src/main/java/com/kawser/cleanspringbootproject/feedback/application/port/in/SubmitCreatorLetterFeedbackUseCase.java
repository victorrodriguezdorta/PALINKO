package com.kawser.cleanspringbootproject.feedback.application.port.in;

import com.kawser.cleanspringbootproject.feedback.application.dto.SubmitFeedbackCommand;

public interface SubmitCreatorLetterFeedbackUseCase {

    void submitCreatorLetterFeedback(SubmitFeedbackCommand command);
}
