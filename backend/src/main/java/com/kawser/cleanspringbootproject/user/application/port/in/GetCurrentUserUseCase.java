package com.kawser.cleanspringbootproject.user.application.port.in;

import com.kawser.cleanspringbootproject.user.application.dto.UserProfileResult;

public interface GetCurrentUserUseCase {

    UserProfileResult getCurrentUser(String username);
}
