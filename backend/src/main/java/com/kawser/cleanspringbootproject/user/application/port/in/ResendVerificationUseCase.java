package com.kawser.cleanspringbootproject.user.application.port.in;

import com.kawser.cleanspringbootproject.user.application.dto.ResendVerificationCommand;

public interface ResendVerificationUseCase {

    void resendVerification(ResendVerificationCommand command);
}
