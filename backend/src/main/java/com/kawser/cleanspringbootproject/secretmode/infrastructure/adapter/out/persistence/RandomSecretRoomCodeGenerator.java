package com.kawser.cleanspringbootproject.secretmode.infrastructure.adapter.out.persistence;

import com.kawser.cleanspringbootproject.secretmode.application.port.out.SecretRoomCodeGenerator;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * 6-character alphanumeric codes prefixed with "SC-" (excluding visually
 * ambiguous characters 0/O, 1/I, same alphabet as
 * game.infrastructure.adapter.out.persistence.RandomRoomCodeGenerator). The
 * prefix lets SecretStompAuthChannelInterceptor tell at a glance whether an
 * incoming STOMP CONNECT's room-code header belongs to secretmode or to the
 * original game, without ever having to query the other mode's repository.
 */
@Component
public class RandomSecretRoomCodeGenerator implements SecretRoomCodeGenerator {

    public static final String PREFIX = "SC-";
    private static final String ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final int CODE_LENGTH = 6;

    private final SecureRandom random = new SecureRandom();

    @Override
    public String generate() {
        StringBuilder code = new StringBuilder(PREFIX.length() + CODE_LENGTH);
        code.append(PREFIX);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }
}
