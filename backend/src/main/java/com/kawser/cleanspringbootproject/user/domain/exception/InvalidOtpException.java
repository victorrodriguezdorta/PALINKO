package com.kawser.cleanspringbootproject.user.domain.exception;

public class InvalidOtpException extends UserDomainException {

    public InvalidOtpException() {
        super("INVALID_OTP", "The verification code is invalid or has expired");
    }
}
