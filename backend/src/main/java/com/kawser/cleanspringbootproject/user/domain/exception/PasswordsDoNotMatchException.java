package com.kawser.cleanspringbootproject.user.domain.exception;

public class PasswordsDoNotMatchException extends UserDomainException {

    public PasswordsDoNotMatchException() {
        super("PASSWORDS_DO_NOT_MATCH", "Password and confirmation do not match");
    }
}
