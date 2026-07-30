package com.kawser.cleanspringbootproject.game.application.service;

import com.kawser.cleanspringbootproject.game.domain.model.GameLanguage;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Derives the "server day" (the UTC calendar date) and a chain-generation
 * seed from it, so every player worldwide who opens the daily challenge on
 * the same UTC date gets byte-identical phases: the seed is a stable hash
 * of the date plus the room's language, fed into a {@link java.util.Random}
 * that {@link com.kawser.cleanspringbootproject.game.application.port.out.ChainWordBank#fullChain(GameLanguage, int, long)}
 * uses in place of true randomness.
 */
final class DailySeed {

    private DailySeed() {
    }

    static LocalDate today() {
        return LocalDate.now(ZoneOffset.UTC);
    }

    static long seedFor(LocalDate utcDate, GameLanguage language) {
        String material = utcDate + "|" + language;
        byte[] hash = sha256(material);
        return ByteBuffer.wrap(hash, 0, Long.BYTES).getLong();
    }

    private static byte[] sha256(String material) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(material.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 must be available on every JVM", e);
        }
    }
}
