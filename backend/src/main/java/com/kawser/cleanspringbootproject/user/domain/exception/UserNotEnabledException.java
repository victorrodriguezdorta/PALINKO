package com.kawser.cleanspringbootproject.user.domain.exception;

public class UserNotEnabledException extends UserDomainException {

    public UserNotEnabledException() {
        super("USER_NOT_ENABLED", "Account is not verified yet");
    }
}
