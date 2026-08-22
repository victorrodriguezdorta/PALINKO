package com.kawser.cleanspringbootproject.user.infrastructure.adapter.out.security;

import com.kawser.cleanspringbootproject.user.application.port.out.OtpGenerator;
import com.kawser.cleanspringbootproject.user.domain.model.OneTimePassword;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * Generates a 6-digit numeric code with SecureRandom (the reference
 * implementation this module is based on used java.util.Random, which is
 * not suitable for anything security-sensitive like an account
 * verification/reset code).
 */
@Component
public class SecureRandomOtpGenerator implements OtpGenerator {

    private final SecureRandom random = new SecureRandom();

    @Override
    public OneTimePassword generate() {
        String otp = String.format("%06d", random.nextInt(1_000_000));
        return new OneTimePassword(otp, LocalDateTime.now());
    }
}
