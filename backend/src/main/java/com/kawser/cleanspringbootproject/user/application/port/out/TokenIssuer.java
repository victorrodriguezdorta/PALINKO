package com.kawser.cleanspringbootproject.user.application.port.out;

import com.kawser.cleanspringbootproject.user.domain.model.User;

import java.util.Optional;

public interface TokenIssuer {

    String generateToken(User user);

    /**
     * Returns the token subject (username) when the token is structurally
     * valid and unexpired, empty otherwise — never throws, and never
     * returns a sentinel like "" for a bad token (that was the source of a
     * NullPointerException in the reference implementation this module is
     * based on: callers must handle "no subject" explicitly instead of
     * being handed a value that looks real).
     */
    Optional<String> validateToken(String token);
}
