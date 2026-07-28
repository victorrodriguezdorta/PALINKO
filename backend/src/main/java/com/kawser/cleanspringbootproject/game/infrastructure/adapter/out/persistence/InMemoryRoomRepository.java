package com.kawser.cleanspringbootproject.game.infrastructure.adapter.out.persistence;

import com.kawser.cleanspringbootproject.game.application.port.out.RoomRepository;
import com.kawser.cleanspringbootproject.game.domain.exception.RoomNotFoundException;
import com.kawser.cleanspringbootproject.game.domain.model.Room;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

/**
 * Holds every Room purely in memory, keyed by room code. A dedicated lock
 * per room (created lazily alongside the room itself) guarantees that a
 * player action and a concurrently firing phase timer for the same room
 * never interleave mid-mutation, while unrelated rooms never block each
 * other.
 */
@Repository
public class InMemoryRoomRepository implements RoomRepository {

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Map<String, ReentrantLock> roomLocks = new ConcurrentHashMap<>();

    @Override
    public Optional<Room> findByCode(String code) {
        return Optional.ofNullable(rooms.get(code));
    }

    @Override
    public boolean existsByCode(String code) {
        return rooms.containsKey(code);
    }

    @Override
    public void save(Room room) {
        rooms.put(room.code(), room);
        roomLocks.computeIfAbsent(room.code(), key -> new ReentrantLock());
    }

    @Override
    public void deleteByCode(String code) {
        rooms.remove(code);
        roomLocks.remove(code);
    }

    @Override
    public List<Room> findAll() {
        return List.copyOf(rooms.values());
    }

    @Override
    public <T> T mutate(String code, Function<Room, T> action) {
        ReentrantLock lock = roomLocks.computeIfAbsent(code, key -> new ReentrantLock());
        lock.lock();
        try {
            Room room = rooms.get(code);
            if (room == null) {
                throw new RoomNotFoundException(code);
            }
            T result = action.apply(room);
            rooms.put(code, room);
            return result;
        } finally {
            lock.unlock();
        }
    }
}
