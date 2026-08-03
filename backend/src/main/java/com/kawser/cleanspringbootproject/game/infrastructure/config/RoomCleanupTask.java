package com.kawser.cleanspringbootproject.game.infrastructure.config;

import com.kawser.cleanspringbootproject.game.application.port.out.PhaseScheduler;
import com.kawser.cleanspringbootproject.game.application.port.out.RoomNotifier;
import com.kawser.cleanspringbootproject.game.application.port.out.RoomRepository;
import com.kawser.cleanspringbootproject.game.domain.exception.RoomNotFoundException;
import com.kawser.cleanspringbootproject.game.domain.model.Room;
import com.kawser.cleanspringbootproject.game.domain.model.RoomStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Sweeps rooms out of memory once they're no longer useful, and — for rooms
 * that are still alive — drops any player whose reconnect grace window has
 * expired:
 *   - A room with zero connected players is deleted immediately, without
 *     waiting out its status TTL — nobody is left to read a lingering
 *     snapshot, so there is nothing gained by keeping it around. (The host
 *     leaving is already handled synchronously in
 *     GameApplicationService.handleDisconnect; this catches every other
 *     way a room can end up empty, e.g. every non-host player dropping
 *     while the host's own session hasn't fired a disconnect event yet.)
 *   - FINISHED/CLOSED rooms get a short grace period so clients can still
 *     read the final snapshot (or, for CLOSED, the "host left" screen).
 *   - A LOBBY nobody ever starts gets a much shorter leash than an idle
 *     in-progress game, since there is nothing at stake yet to protect.
 *   - A disconnected non-host player keeps their seat and score for a
 *     while in case they reconnect, but not forever.
 */
@Component
public class RoomCleanupTask {

    private final RoomRepository roomRepository;
    private final RoomNotifier roomNotifier;
    private final PhaseScheduler phaseScheduler;
    private final Duration finishedRoomTtl;
    private final Duration lobbyRoomTtl;
    private final Duration idleRoomTtl;
    private final Duration playerReconnectWindow;

    public RoomCleanupTask(
            RoomRepository roomRepository,
            RoomNotifier roomNotifier,
            PhaseScheduler phaseScheduler,
            @Value("${game.cleanup.finished-room-ttl-seconds}") long finishedRoomTtlSeconds,
            @Value("${game.cleanup.lobby-room-ttl-seconds}") long lobbyRoomTtlSeconds,
            @Value("${game.cleanup.idle-room-ttl-seconds}") long idleRoomTtlSeconds,
            @Value("${game.cleanup.player-reconnect-window-seconds}") long playerReconnectWindowSeconds) {
        this.roomRepository = roomRepository;
        this.roomNotifier = roomNotifier;
        this.phaseScheduler = phaseScheduler;
        this.finishedRoomTtl = Duration.ofSeconds(finishedRoomTtlSeconds);
        this.lobbyRoomTtl = Duration.ofSeconds(lobbyRoomTtlSeconds);
        this.idleRoomTtl = Duration.ofSeconds(idleRoomTtlSeconds);
        this.playerReconnectWindow = Duration.ofSeconds(playerReconnectWindowSeconds);
    }

    @Scheduled(fixedDelayString = "${game.cleanup.sweep-interval-seconds}000")
    public void sweep() {
        Instant now = Instant.now();
        for (Room room : roomRepository.findAll()) {
            if (deleteIfExpired(room, now)) {
                continue;
            }
            purgeExpiredDisconnectedPlayers(room, now);
        }
    }

    private boolean deleteIfExpired(Room room, Instant now) {
        if (room.connectedHumanPlayerCount() == 0) {
            phaseScheduler.cancel(room.code());
            roomRepository.deleteByCode(room.code());
            return true;
        }
        Duration idleFor = Duration.between(room.lastActivityAt(), now);
        Duration ttl = ttlFor(room.status());
        if (idleFor.compareTo(ttl) < 0) {
            return false;
        }
        phaseScheduler.cancel(room.code());
        roomRepository.deleteByCode(room.code());
        return true;
    }

    private Duration ttlFor(RoomStatus status) {
        return switch (status) {
            case FINISHED, CLOSED -> finishedRoomTtl;
            case LOBBY -> lobbyRoomTtl;
            case IN_PROGRESS -> idleRoomTtl;
        };
    }

    private void purgeExpiredDisconnectedPlayers(Room room, Instant now) {
        try {
            Room updated = roomRepository.mutate(room.code(), r -> {
                boolean removedAny = r.removeExpiredDisconnectedPlayers(now, playerReconnectWindow);
                return removedAny ? r : null;
            });
            if (updated != null) {
                roomNotifier.notifyRoomUpdated(updated);
            }
        } catch (RoomNotFoundException ignored) {
            // Deleted concurrently (e.g. the host just left) between
            // findAll() and this mutate — nothing left to sweep.
        }
    }
}
