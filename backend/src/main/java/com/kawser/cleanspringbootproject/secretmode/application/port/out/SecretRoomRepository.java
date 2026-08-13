package com.kawser.cleanspringbootproject.secretmode.application.port.out;

import com.kawser.cleanspringbootproject.secretmode.domain.model.SecretRoom;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Stores secret-mode rooms purely in memory, mirroring
 * game.application.port.out.RoomRepository exactly (mutate() as the single
 * synchronized entry point for every mutation) but kept as its own
 * interface/implementation so secretmode never depends on Room being the
 * concrete aggregate type.
 */
public interface SecretRoomRepository {

    Optional<SecretRoom> findByCode(String code);

    boolean existsByCode(String code);

    void save(SecretRoom room);

    void deleteByCode(String code);

    List<SecretRoom> findAll();

    <T> T mutate(String code, Function<SecretRoom, T> action);
}
