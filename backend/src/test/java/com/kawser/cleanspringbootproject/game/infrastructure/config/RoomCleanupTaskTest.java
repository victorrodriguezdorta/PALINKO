package com.kawser.cleanspringbootproject.game.infrastructure.config;

import com.kawser.cleanspringbootproject.game.application.port.out.PhaseScheduler;
import com.kawser.cleanspringbootproject.game.application.port.out.RoomNotifier;
import com.kawser.cleanspringbootproject.game.domain.model.GameLanguage;
import com.kawser.cleanspringbootproject.game.domain.model.Player;
import com.kawser.cleanspringbootproject.game.domain.model.Room;
import com.kawser.cleanspringbootproject.game.domain.model.RoomSettings;
import com.kawser.cleanspringbootproject.game.domain.model.RoomStatus;
import com.kawser.cleanspringbootproject.game.infrastructure.adapter.out.persistence.InMemoryRoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RoomCleanupTaskTest {

    private static final long FINISHED_TTL_SECONDS = 300;
    private static final long LOBBY_TTL_SECONDS = 600;
    private static final long IDLE_TTL_SECONDS = 1800;
    private static final long EMPTY_TTL_SECONDS = 120;
    private static final long HOST_RECONNECT_WINDOW_SECONDS = 120;
    private static final long PLAYER_RECONNECT_WINDOW_SECONDS = 300;

    private InMemoryRoomRepository roomRepository;
    private RoomNotifier roomNotifier;
    private PhaseScheduler phaseScheduler;
    private RoomCleanupTask task;

    @BeforeEach
    void setUp() {
        roomRepository = new InMemoryRoomRepository();
        roomNotifier = mock(RoomNotifier.class);
        phaseScheduler = mock(PhaseScheduler.class);
        task = new RoomCleanupTask(
                roomRepository, roomNotifier, phaseScheduler,
                FINISHED_TTL_SECONDS, LOBBY_TTL_SECONDS, IDLE_TTL_SECONDS,
                EMPTY_TTL_SECONDS, HOST_RECONNECT_WINDOW_SECONDS, PLAYER_RECONNECT_WINDOW_SECONDS);
    }

    private Room roomWithHostAndGuest(Instant now) {
        Room room = Room.create(
                "CODE1", RoomSettings.defaults(GameLanguage.SPANISH),
                Player.host("host", "t0", "Host", "seed-host"), now);
        room.addPlayer(Player.guest("guest", "t1", "Guest", "seed-guest"), now);
        roomRepository.save(room);
        return room;
    }

    @Test
    void hostDisconnectingBrieflyDoesNotCloseTheRoom() {
        Instant now = Instant.now();
        Room room = roomWithHostAndGuest(now);
        room.markDisconnected("host", now);

        task.sweep();

        assertThat(roomRepository.findByCode("CODE1")).isPresent();
        assertThat(roomRepository.findByCode("CODE1").orElseThrow().status()).isNotEqualTo(RoomStatus.CLOSED);
    }

    @Test
    void hostStillGoneAfterTheGraceWindowClosesAndDeletesTheRoom() {
        Instant disconnectedAt = Instant.now().minusSeconds(HOST_RECONNECT_WINDOW_SECONDS + 1);
        Room room = roomWithHostAndGuest(disconnectedAt);
        room.markDisconnected("host", disconnectedAt);

        task.sweep();

        assertThat(roomRepository.findByCode("CODE1")).isEmpty();
        verify(phaseScheduler).cancel("CODE1");
        verify(roomNotifier).notifyRoomUpdated(org.mockito.ArgumentMatchers.argThat(
                r -> r.status() == RoomStatus.CLOSED));
    }

    @Test
    void hostReconnectingWithinTheWindowKeepsTheRoomAlive() {
        Instant disconnectedAt = Instant.now().minusSeconds(HOST_RECONNECT_WINDOW_SECONDS - 5);
        Room room = roomWithHostAndGuest(disconnectedAt);
        room.markDisconnected("host", disconnectedAt);
        room.markReconnected("host", Instant.now());

        task.sweep();

        assertThat(roomRepository.findByCode("CODE1")).isPresent();
        assertThat(roomRepository.findByCode("CODE1").orElseThrow().status()).isNotEqualTo(RoomStatus.CLOSED);
    }

    @Test
    void anEmptyRoomSurvivesWithinItsGracePeriod() {
        Instant now = Instant.now();
        Room room = roomWithHostAndGuest(now);
        room.markDisconnected("host", now);
        room.markDisconnected("guest", now);

        task.sweep();

        assertThat(roomRepository.findByCode("CODE1")).isPresent();
    }

    @Test
    void anEmptyRoomIsDeletedOnceItsGracePeriodElapses() {
        Instant emptiedAt = Instant.now().minusSeconds(EMPTY_TTL_SECONDS + 1);
        Room room = roomWithHostAndGuest(emptiedAt);
        room.markDisconnected("host", emptiedAt);
        room.markDisconnected("guest", emptiedAt);

        task.sweep();

        assertThat(roomRepository.findByCode("CODE1")).isEmpty();
        verify(phaseScheduler).cancel("CODE1");
    }

    @Test
    void aRoomAlreadyClosedIsNotReClosedByTheHostGraceCheck() {
        Instant now = Instant.now();
        Room room = roomWithHostAndGuest(now);
        room.markDisconnected("host", now.minusSeconds(HOST_RECONNECT_WINDOW_SECONDS + 1));
        room.close(now.minusSeconds(FINISHED_TTL_SECONDS - 1));

        task.sweep();

        // Still within its own CLOSED TTL, so the sweep leaves it alone
        // rather than tripping the host-grace path a second time.
        assertThat(roomRepository.findByCode("CODE1")).isPresent();
    }
}
