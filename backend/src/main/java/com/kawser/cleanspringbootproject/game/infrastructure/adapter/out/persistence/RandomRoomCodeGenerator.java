package com.kawser.cleanspringbootproject.game.infrastructure.adapter.out.persistence;

import com.kawser.cleanspringbootproject.game.application.port.out.RoomCodeGenerator;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * 6-character alphanumeric codes, excluding visually ambiguous characters
 * (0/O, 1/I) so players can read a code aloud or off a screen reliably.
 */
@Component
public class RandomRoomCodeGenerator implements RoomCodeGenerator {

    private static final String ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final int CODE_LENGTH = 6;

    private final SecureRandom random = new SecureRandom();

    @Override
    public String generate() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }
}
