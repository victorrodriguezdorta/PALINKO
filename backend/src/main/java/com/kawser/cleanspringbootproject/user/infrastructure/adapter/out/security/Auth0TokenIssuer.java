package com.kawser.cleanspringbootproject.user.infrastructure.adapter.out.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.kawser.cleanspringbootproject.user.application.port.out.TokenIssuer;
import com.kawser.cleanspringbootproject.user.domain.model.User;
import com.kawser.cleanspringbootproject.user.infrastructure.config.AuthProperties;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
public class Auth0TokenIssuer implements TokenIssuer {

    private static final String ISSUER = "palinko-auth";

    private final AuthProperties authProperties;

    public Auth0TokenIssuer(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    @Override
    public String generateToken(User user) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(authProperties.jwtSecret());
            return JWT.create()
                    .withIssuer(ISSUER)
                    .withSubject(user.username())
                    .withExpiresAt(Instant.now().plusSeconds(authProperties.jwtExpirationSeconds()))
                    .sign(algorithm);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate JWT", e);
        }
    }

    @Override
    public Optional<String> validateToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(authProperties.jwtSecret());
            String subject = JWT.require(algorithm)
                    .withIssuer(ISSUER)
                    .build()
                    .verify(token)
                    .getSubject();
            return Optional.ofNullable(subject);
        } catch (JWTVerificationException e) {
            return Optional.empty();
        }
    }
}
