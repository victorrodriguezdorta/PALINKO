package com.kawser.cleanspringbootproject.user.infrastructure.adapter.out.security;

import com.kawser.cleanspringbootproject.user.application.port.out.AvatarSeedGenerator;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Produces a short random seed at signup time, fed straight into the same
 * dicebear-based rendering the frontend already uses for in-game player
 * avatars (see frontend/src/utils/avatar.ts) — not user-chosen yet, just a
 * starting point to be made customizable later.
 */
@Component
public class RandomAvatarSeedGenerator implements AvatarSeedGenerator {

    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final int LENGTH = 10;

    private final SecureRandom random = new SecureRandom();

    @Override
    public String generate() {
        StringBuilder seed = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            seed.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return seed.toString();
    }
}
