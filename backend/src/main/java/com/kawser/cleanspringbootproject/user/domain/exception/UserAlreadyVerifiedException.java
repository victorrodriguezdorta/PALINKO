package com.kawser.cleanspringbootproject.user.domain.exception;

public class UserAlreadyVerifiedException extends UserDomainException {

    public UserAlreadyVerifiedException() {
        super("USER_ALREADY_VERIFIED", "Account is already verified");
    }
}
