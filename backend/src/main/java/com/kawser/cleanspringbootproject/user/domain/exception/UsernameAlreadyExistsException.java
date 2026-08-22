package com.kawser.cleanspringbootproject.user.domain.exception;

import java.util.Map;

public class UsernameAlreadyExistsException extends UserDomainException {

    public UsernameAlreadyExistsException(String username) {
        super("USERNAME_ALREADY_EXISTS", "Username already taken: " + username, Map.of("username", username));
    }
}
