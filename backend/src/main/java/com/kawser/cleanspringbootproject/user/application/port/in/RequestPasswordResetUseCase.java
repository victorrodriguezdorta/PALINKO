package com.kawser.cleanspringbootproject.user.application.port.in;

import com.kawser.cleanspringbootproject.user.application.dto.RequestPasswordResetCommand;

public interface RequestPasswordResetUseCase {

    void requestPasswordReset(RequestPasswordResetCommand command);
}
