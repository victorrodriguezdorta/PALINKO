package com.kawser.cleanspringbootproject.user.application.port.in;

import com.kawser.cleanspringbootproject.user.application.dto.LoginCommand;
import com.kawser.cleanspringbootproject.user.application.dto.LoginResult;

public interface LoginUseCase {

    LoginResult login(LoginCommand command);
}
