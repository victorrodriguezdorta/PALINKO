package com.kawser.cleanspringbootproject.game.infrastructure.config;

import com.kawser.cleanspringbootproject.game.application.port.out.PhaseScheduler;
import com.kawser.cleanspringbootproject.game.application.port.out.RoomRepository;
import com.kawser.cleanspringbootproject.game.domain.model.Room;
import com.kawser.cleanspringbootproject.game.domain.model.RoomStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Sweeps rooms out of memory once they're no longer useful: FINISHED rooms
 * get a short grace period so clients can still read the final snapshot,
 * while LOBBY/IN_PROGRESS rooms with no recent activity (host created it
 * and never started, or everyone left mid-game) get a longer one.
 */
@Component
public class RoomCleanupTask {

    private final RoomRepository roomRepository;
    private final PhaseScheduler phaseScheduler;
    private final Duration finishedRoomTtl;
    private final Duration idleRoomTtl;

    public RoomCleanupTask(
            RoomRepository roomRepository,
            PhaseScheduler phaseScheduler,
            @Value("${game.cleanup.finished-room-ttl-seconds}") long finishedRoomTtlSeconds,
            @Value("${game.cleanup.idle-room-ttl-seconds}") long idleRoomTtlSeconds) {
        this.roomRepository = roomRepository;
        this.phaseScheduler = phaseScheduler;
        this.finishedRoomTtl = Duration.ofSeconds(finishedRoomTtlSeconds);
        this.idleRoomTtl = Duration.ofSeconds(idleRoomTtlSeconds);
    }

    @Scheduled(fixedDelayString = "${game.cleanup.sweep-interval-seconds}000")
    public void sweep() {
        Instant now = Instant.now();
        for (Room room : roomRepository.findAll()) {
            Duration idleFor = Duration.between(room.lastActivityAt(), now);
            boolean ended = room.status() == RoomStatus.FINISHED || room.status() == RoomStatus.CLOSED;
            Duration ttl = ended ? finishedRoomTtl : idleRoomTtl;
            if (idleFor.compareTo(ttl) >= 0) {
                phaseScheduler.cancel(room.code());
                roomRepository.deleteByCode(room.code());
            }
        }
    }
}
