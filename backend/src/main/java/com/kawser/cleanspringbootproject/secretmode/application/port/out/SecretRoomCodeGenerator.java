package com.kawser.cleanspringbootproject.secretmode.application.port.out;

/**
 * Generates short human-shareable secret-mode room codes. Uniqueness
 * against existing rooms is the caller's responsibility (retry on
 * collision), same contract as game.application.port.out.RoomCodeGenerator.
 */
public interface SecretRoomCodeGenerator {

    String generate();
}
