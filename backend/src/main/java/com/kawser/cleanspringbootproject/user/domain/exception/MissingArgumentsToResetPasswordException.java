package com.kawser.cleanspringbootproject.user.domain.exception;

public class MissingArgumentsToResetPasswordException extends UserDomainException {

    public MissingArgumentsToResetPasswordException() {
        super("MISSING_RESET_ARGUMENTS", "Missing required arguments to reset the password");
    }
}
