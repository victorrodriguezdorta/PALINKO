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
 *   - A room with zero connected players is given a short grace period
 *     (emptyRoomTtl) rather than being deleted on the spot: a lone
 *     remaining player (often the host) may just be suffering a momentary
 *     network blip and come straight back, and deleting the room out from
 *     under them would make that reconnect fail for no reason. Once truly
 *     nobody returns in time, it's swept like anything else.
 *   - A room whose host has been disconnected for longer than
 *     hostReconnectWindow is closed (Room.close) and deleted, even if
 *     other guests are still connected — with nobody able to start or
 *     otherwise steer the game, it can't usefully continue. A host who
 *     reconnects within the window (see GameApplicationService.reconnect)
 *     keeps the room exactly as it was, so a brief disconnect no longer
 *     evicts every other player.
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
    private final Duration emptyRoomTtl;
    private final Duration hostReconnectWindow;
    private final Duration playerReconnectWindow;

    public RoomCleanupTask(
            RoomRepository roomRepository,
            RoomNotifier roomNotifier,
            PhaseScheduler phaseScheduler,
            @Value("${game.cleanup.finished-room-ttl-seconds}") long finishedRoomTtlSeconds,
            @Value("${game.cleanup.lobby-room-ttl-seconds}") long lobbyRoomTtlSeconds,
            @Value("${game.cleanup.idle-room-ttl-seconds}") long idleRoomTtlSeconds,
            @Value("${game.cleanup.empty-room-ttl-seconds}") long emptyRoomTtlSeconds,
            @Value("${game.cleanup.host-reconnect-window-seconds}") long hostReconnectWindowSeconds,
            @Value("${game.cleanup.player-reconnect-window-seconds}") long playerReconnectWindowSeconds) {
        this.roomRepository = roomRepository;
        this.roomNotifier = roomNotifier;
        this.phaseScheduler = phaseScheduler;
        this.finishedRoomTtl = Duration.ofSeconds(finishedRoomTtlSeconds);
        this.lobbyRoomTtl = Duration.ofSeconds(lobbyRoomTtlSeconds);
        this.idleRoomTtl = Duration.ofSeconds(idleRoomTtlSeconds);
        this.emptyRoomTtl = Duration.ofSeconds(emptyRoomTtlSeconds);
        this.hostReconnectWindow = Duration.ofSeconds(hostReconnectWindowSeconds);
        this.playerReconnectWindow = Duration.ofSeconds(playerReconnectWindowSeconds);
    }

    @Scheduled(fixedDelayString = "${game.cleanup.sweep-interval-seconds}000")
    public void sweep() {
        Instant now = Instant.now();
        for (Room room : roomRepository.findAll()) {
            if (deleteIfExpired(room, now)) {
                continue;
            }
            if (closeIfHostGraceExpired(room, now)) {
                continue;
            }
            purgeExpiredDisconnectedPlayers(room, now);
        }
    }

    private boolean deleteIfExpired(Room room, Instant now) {
        if (room.connectedHumanPlayerCount() == 0) {
            Duration emptyFor = Duration.between(room.lastActivityAt(), now);
            if (emptyFor.compareTo(emptyRoomTtl) < 0) {
                return false;
            }
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

    /**
     * Closes and deletes a room whose host has been gone longer than
     * hostReconnectWindow — mirrors deleteIfExpired's shape (cancel the
     * phase timer, delete, report whether it happened) but is host-specific
     * rather than idle-time-based, since a room can otherwise be perfectly
     * "active" (guests still connected and interacting) while nobody can
     * steer it.
     */
    private boolean closeIfHostGraceExpired(Room room, Instant now) {
        if (room.status() == RoomStatus.FINISHED || room.status() == RoomStatus.CLOSED) {
            return false;
        }
        if (!room.isHostReconnectWindowExpired(now, hostReconnectWindow)) {
            return false;
        }
        try {
            Room closed = roomRepository.mutate(room.code(), r -> {
                r.close(now);
                return r;
            });
            roomNotifier.notifyRoomUpdated(closed);
        } catch (RoomNotFoundException ignored) {
            // Deleted concurrently since findAll() — nothing left to close.
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
