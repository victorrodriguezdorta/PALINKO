package com.kawser.cleanspringbootproject.user.domain.model;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * A short-lived one-time code embedded directly on the owning user (see
 * UserJpaEntity/OneTimePasswordEmbeddable) rather than kept in a separate
 * table — the same code is reused for both email verification and password
 * reset, one at a time, and is cleared once consumed.
 */
public record OneTimePassword(String otp, LocalDateTime otpGenerationTime) {

    private static final int MINUTES_TO_EXPIRE = 5;

    public OneTimePassword {
        if (otp == null || otpGenerationTime == null) {
            throw new IllegalArgumentException("otp and otpGenerationTime must not be null");
        }
    }

    public boolean matches(String candidate) {
        return otp.equals(candidate);
    }

    public boolean isExpired(LocalDateTime now) {
        return Duration.between(otpGenerationTime, now).toMinutes() >= MINUTES_TO_EXPIRE;
    }

    public boolean isValid(String candidate, LocalDateTime now) {
        return matches(candidate) && !isExpired(now);
    }
}
