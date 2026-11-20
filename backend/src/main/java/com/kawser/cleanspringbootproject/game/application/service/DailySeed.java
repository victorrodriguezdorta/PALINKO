package com.kawser.cleanspringbootproject.game.application.service;

import com.kawser.cleanspringbootproject.game.domain.model.GameLanguage;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Provides the UTC date and stable date/language seed used by daily
 * challenges.
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
