package com.kawser.cleanspringbootproject.user.domain.exception;

public class InvalidCredentialsException extends UserDomainException {

    public InvalidCredentialsException() {
        super("INVALID_CREDENTIALS", "Invalid username or password");
    }
}
