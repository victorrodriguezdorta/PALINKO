package com.kawser.cleanspringbootproject.user.application.dto;

/**
 * Always carries a freshly issued token — the JWT subject is the username
 * (see Auth0TokenIssuer), so any profile update that changes the username
 * would otherwise leave the caller holding a token whose subject no longer
 * resolves to any account, breaking every subsequent authenticated request
 * until they log in again. Re-issuing unconditionally is simpler than
 * bifurcating on whether the username actually changed.
 */
public record UpdateProfileResult(String token, UserProfileResult profile) {
}
