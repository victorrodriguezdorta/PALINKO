package com.kawser.cleanspringbootproject.user.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Plain @Value-backed bean rather than @ConfigurationProperties, to match
 * the rest of the codebase's convention (see RateLimitFilter, WebCorsConfig).
 */
@Component
public class AuthProperties {

    private final String jwtSecret;
    private final long jwtExpirationSeconds;
    private final boolean emailVerificationEnabled;
    private final String frontendBaseUrl;

    public AuthProperties(
            @Value("${app.auth.jwt.secret}") String jwtSecret,
            @Value("${app.auth.jwt.expiration-seconds}") long jwtExpirationSeconds,
            @Value("${app.auth.email-verification.enabled}") boolean emailVerificationEnabled,
            @Value("${app.auth.frontend-base-url}") String frontendBaseUrl) {
        this.jwtSecret = jwtSecret;
        this.jwtExpirationSeconds = jwtExpirationSeconds;
        this.emailVerificationEnabled = emailVerificationEnabled;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    public String jwtSecret() {
        return jwtSecret;
    }

    public long jwtExpirationSeconds() {
        return jwtExpirationSeconds;
    }

    public boolean emailVerificationEnabled() {
        return emailVerificationEnabled;
    }

    public String frontendBaseUrl() {
        return frontendBaseUrl;
    }
}
