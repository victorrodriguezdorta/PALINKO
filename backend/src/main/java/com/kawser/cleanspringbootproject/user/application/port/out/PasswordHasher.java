package com.kawser.cleanspringbootproject.user.application.port.out;

/**
 * Out-port wrapping Spring Security's PasswordEncoder bean so the
 * application service never instantiates a BCryptPasswordEncoder by hand
 * (a bug in the reference implementation this module is based on) — the
 * single bean declared in SecurityConfig is always injected instead.
 */
public interface PasswordHasher {

    String hash(String rawPassword);

    boolean matches(String rawPassword, String hash);
}
