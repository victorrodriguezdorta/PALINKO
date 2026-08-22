package com.kawser.cleanspringbootproject.user.domain.exception;

/**
 * No identifying data (email/username) in errorArgs on purpose — this
 * exception is raised from email-driven flows (verify/resend/reset), and
 * echoing back which email doesn't exist would let an attacker enumerate
 * registered accounts.
 */
public class UserNotFoundException extends UserDomainException {

    public UserNotFoundException() {
        super("USER_NOT_FOUND", "User not found");
    }
}
