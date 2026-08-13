package com.kawser.cleanspringbootproject.secretmode.infrastructure.adapter.out.persistence;

import com.kawser.cleanspringbootproject.secretmode.application.port.out.SecretRoomRepository;
import com.kawser.cleanspringbootproject.secretmode.domain.exception.SecretRoomNotFoundException;
import com.kawser.cleanspringbootproject.secretmode.domain.model.SecretRoom;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

/**
 * Holds every SecretRoom purely in memory, keyed by room code - same
 * per-room-lock pattern as
 * game.infrastructure.adapter.out.persistence.InMemoryRoomRepository, kept
 * as an independent map/lock table rather than sharing that one so the two
 * modes' rooms can never collide or block each other.
 */
@Repository
public class InMemorySecretRoomRepository implements SecretRoomRepository {

    private final Map<String, SecretRoom> rooms = new ConcurrentHashMap<>();
    private final Map<String, ReentrantLock> roomLocks = new ConcurrentHashMap<>();

    @Override
    public Optional<SecretRoom> findByCode(String code) {
        return Optional.ofNullable(rooms.get(code));
    }

    @Override
    public boolean existsByCode(String code) {
        return rooms.containsKey(code);
    }

    @Override
    public void save(SecretRoom room) {
        rooms.put(room.code(), room);
        roomLocks.computeIfAbsent(room.code(), key -> new ReentrantLock());
    }

    @Override
    public void deleteByCode(String code) {
        rooms.remove(code);
        roomLocks.remove(code);
    }

    @Override
    public List<SecretRoom> findAll() {
        return List.copyOf(rooms.values());
    }

    @Override
    public <T> T mutate(String code, Function<SecretRoom, T> action) {
        ReentrantLock lock = roomLocks.computeIfAbsent(code, key -> new ReentrantLock());
        lock.lock();
        try {
            SecretRoom room = rooms.get(code);
            if (room == null) {
                throw new SecretRoomNotFoundException(code);
            }
            T result = action.apply(room);
            rooms.put(code, room);
            return result;
        } finally {
            lock.unlock();
        }
    }
}
