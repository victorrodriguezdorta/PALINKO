package com.kawser.cleanspringbootproject.user.infrastructure.adapter.out.security;

import com.kawser.cleanspringbootproject.user.application.port.out.PasswordHasher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Delegates to the single PasswordEncoder bean declared in SecurityConfig —
 * never instantiates a new BCryptPasswordEncoder() by hand (a bug in the
 * reference implementation this module is based on).
 */
@Component
public class BCryptPasswordHasher implements PasswordHasher {

    private final PasswordEncoder passwordEncoder;

    public BCryptPasswordHasher(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public String hash(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String hash) {
        return passwordEncoder.matches(rawPassword, hash);
    }
}
