package com.kawser.cleanspringbootproject.user.application.port.in;

import com.kawser.cleanspringbootproject.user.application.dto.SignupCommand;

public interface SignupUseCase {

    void signup(SignupCommand command);
}
