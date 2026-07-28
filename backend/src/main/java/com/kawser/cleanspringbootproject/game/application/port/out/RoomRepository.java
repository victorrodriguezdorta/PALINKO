package com.kawser.cleanspringbootproject.game.application.port.out;

import com.kawser.cleanspringbootproject.game.domain.model.Room;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Stores rooms purely in memory. mutate() is the only mutation entry point:
 * it acquires the per-room lock, applies the given action, persists the
 * result and releases the lock — so every use case that changes a Room goes
 * through the same synchronization, without each call site having to
 * remember to do it itself.
 */
public interface RoomRepository {

    Optional<Room> findByCode(String code);

    boolean existsByCode(String code);

    void save(Room room);

    void deleteByCode(String code);

    List<Room> findAll();

    <T> T mutate(String code, Function<Room, T> action);
}
