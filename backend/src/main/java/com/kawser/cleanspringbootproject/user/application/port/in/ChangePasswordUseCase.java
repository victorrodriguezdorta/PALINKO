package com.kawser.cleanspringbootproject.user.application.port.in;

import com.kawser.cleanspringbootproject.user.application.dto.ChangePasswordCommand;

public interface ChangePasswordUseCase {

    void changePassword(ChangePasswordCommand command);
}
