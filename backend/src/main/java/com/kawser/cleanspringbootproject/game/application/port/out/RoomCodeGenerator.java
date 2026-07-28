package com.kawser.cleanspringbootproject.game.application.port.out;

/**
 * Generates short human-shareable room codes. Uniqueness against existing
 * rooms is the caller's responsibility (retry on collision).
 */
public interface RoomCodeGenerator {

    String generate();
}
