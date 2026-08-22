package com.kawser.cleanspringbootproject.user.domain.exception;

/**
 * errorArgs deliberately omits the email itself — the raw address must
 * never round-trip back to the client (see the "email is never public"
 * requirement enforced across every response DTO in this module).
 */
public class EmailAlreadyExistsException extends UserDomainException {

    public EmailAlreadyExistsException() {
        super("EMAIL_ALREADY_EXISTS", "This email is already registered");
    }
}
