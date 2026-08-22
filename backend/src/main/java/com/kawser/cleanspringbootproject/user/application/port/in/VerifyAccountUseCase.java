package com.kawser.cleanspringbootproject.user.application.port.in;

import com.kawser.cleanspringbootproject.user.application.dto.VerifyAccountCommand;

public interface VerifyAccountUseCase {

    void verifyAccount(VerifyAccountCommand command);
}
