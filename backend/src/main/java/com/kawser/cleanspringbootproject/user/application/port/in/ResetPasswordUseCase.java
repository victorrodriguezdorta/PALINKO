package com.kawser.cleanspringbootproject.user.application.port.in;

import com.kawser.cleanspringbootproject.user.application.dto.ResetPasswordCommand;

public interface ResetPasswordUseCase {

    void resetPassword(ResetPasswordCommand command);
}
