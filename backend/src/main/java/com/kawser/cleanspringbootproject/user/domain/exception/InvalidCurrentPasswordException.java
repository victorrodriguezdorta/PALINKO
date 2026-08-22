package com.kawser.cleanspringbootproject.user.domain.exception;

public class InvalidCurrentPasswordException extends UserDomainException {

    public InvalidCurrentPasswordException() {
        super("INVALID_CURRENT_PASSWORD", "Current password is incorrect");
    }
}
