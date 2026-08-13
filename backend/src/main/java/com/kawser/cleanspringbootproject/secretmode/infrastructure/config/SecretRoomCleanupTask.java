package com.kawser.cleanspringbootproject.secretmode.infrastructure.config;

import com.kawser.cleanspringbootproject.secretmode.application.port.out.SecretRoomNotifier;
import com.kawser.cleanspringbootproject.secretmode.application.port.out.SecretRoomRepository;
import com.kawser.cleanspringbootproject.secretmode.domain.model.SecretRoom;
import com.kawser.cleanspringbootproject.secretmode.domain.model.SecretRoomStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Sweeps secret-mode rooms out of memory once they're no longer useful,
 * mirroring game.infrastructure.config.RoomCleanupTask's TTL scheme but
 * simplified: a solo room has no host/guest distinction, so there is no
 * "host reconnect grace" case to handle - a room with nobody connected is
 * just given a short grace period before being deleted (in case of a
 * momentary network blip), and a finished (SOLVED/GAVE_UP) or CLOSED room
 * gets a short grace period so the client can still read the final
 * snapshot before it's gone. When a player leaves for good, everything the
 * room held (attempts, hints used, solution guesses) disappears with it, as
 * required.
 */
@Component
public class SecretRoomCleanupTask {

    private final SecretRoomRepository secretRoomRepository;
    private final SecretRoomNotifier secretRoomNotifier;
    private final Duration finishedRoomTtl;
    private final Duration idleRoomTtl;
    private final Duration emptyRoomTtl;
    private final Duration playerReconnectWindow;

    public SecretRoomCleanupTask(
            SecretRoomRepository secretRoomRepository,
            SecretRoomNotifier secretRoomNotifier,
            @Value("${secretgame.cleanup.finished-room-ttl-seconds}") long finishedRoomTtlSeconds,
            @Value("${secretgame.cleanup.idle-room-ttl-seconds}") long idleRoomTtlSeconds,
            @Value("${secretgame.cleanup.empty-room-ttl-seconds}") long emptyRoomTtlSeconds,
            @Value("${secretgame.cleanup.player-reconnect-window-seconds}") long playerReconnectWindowSeconds) {
        this.secretRoomRepository = secretRoomRepository;
        this.secretRoomNotifier = secretRoomNotifier;
        this.finishedRoomTtl = Duration.ofSeconds(finishedRoomTtlSeconds);
        this.idleRoomTtl = Duration.ofSeconds(idleRoomTtlSeconds);
        this.emptyRoomTtl = Duration.ofSeconds(emptyRoomTtlSeconds);
        this.playerReconnectWindow = Duration.ofSeconds(playerReconnectWindowSeconds);
    }

    @Scheduled(fixedDelayString = "${secretgame.cleanup.sweep-interval-seconds}000")
    public void sweep() {
        Instant now = Instant.now();
        for (SecretRoom room : secretRoomRepository.findAll()) {
            closeIfPlayerGoneForGood(room, now);
            deleteIfExpired(room, now);
        }
    }

    /**
     * A disconnected player who hasn't come back within the reconnect
     * window forfeits the room for good - unlike the original game (which
     * only tears a room down on the host's own grace window expiring,
     * since guests can be dropped individually), a solo secretmode room
     * has exactly one player, so their reconnect window expiring closes
     * the whole room, same as Room.close.
     */
    private void closeIfPlayerGoneForGood(SecretRoom room, Instant now) {
        if (room.status() != SecretRoomStatus.IN_PROGRESS) {
            return;
        }
        if (room.player().isConnected() || !room.isReconnectWindowExpired(now, playerReconnectWindow)) {
            return;
        }
        try {
            SecretRoom closed = secretRoomRepository.mutate(room.code(), r -> {
                r.close(now);
                return r;
            });
            secretRoomNotifier.notifyRoomUpdated(closed);
        } catch (RuntimeException ignored) {
            // Deleted concurrently since findAll() - nothing left to close.
        }
    }

    private void deleteIfExpired(SecretRoom room, Instant now) {
        Duration idleFor = Duration.between(room.lastActivityAt(), now);
        Duration ttl = room.player().isConnected() ? ttlFor(room.status()) : emptyRoomTtl;
        if (idleFor.compareTo(ttl) < 0) {
            return;
        }
        secretRoomRepository.deleteByCode(room.code());
    }

    private Duration ttlFor(SecretRoomStatus status) {
        return switch (status) {
            case SOLVED, GAVE_UP, CLOSED -> finishedRoomTtl;
            case IN_PROGRESS -> idleRoomTtl;
        };
    }
}
