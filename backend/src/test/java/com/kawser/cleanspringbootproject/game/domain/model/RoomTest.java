package com.kawser.cleanspringbootproject.game.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RoomTest {

    // RoomSettings.defaults() now defaults to 3 phases, so start() needs a
    // matching 3-entry chain wherever the room was left on its defaults.
    private static final List<WordSet> DEFAULT_PHASE_WORD_SETS = List.of(
            new WordSet("Bolígrafo", "Cama", "Océano"),
            new WordSet("Cama", "Fresa", "Coche"),
            new WordSet("Fresa", "Nube", "Tornillo"));

    private Room newRoomWithHostAndTwoGuests() {
        Room room = Room.create(
                "CODE1", RoomSettings.defaults(GameLanguage.SPANISH), Player.host("host", "t0", "Host"), Instant.now());
        room.addPlayer(Player.guest("guestA", "t1", "GuestA"), Instant.now());
        room.addPlayer(Player.guest("guestB", "t2", "GuestB"), Instant.now());
        return room;
    }

    @Test
    void removeExpiredDisconnectedPlayersDropsOnlyStaleNonHostDisconnections() {
        Room room = newRoomWithHostAndTwoGuests();
        Instant now = Instant.now();
        room.markDisconnected("guestA", now.minusSeconds(400));
        room.markDisconnected("guestB", now.minusSeconds(10));
        room.markDisconnected("host", now.minusSeconds(400));

        boolean removedAny = room.removeExpiredDisconnectedPlayers(now, Duration.ofSeconds(300));

        assertThat(removedAny).isTrue();
        assertThat(room.findPlayer("guestA")).isEmpty();
        assertThat(room.findPlayer("guestB")).isPresent();
        assertThat(room.findPlayer("host")).isPresent();
    }

    @Test
    void reconnectingClearsTheDisconnectClockSoTheWindowNeverExpires() {
        Room room = newRoomWithHostAndTwoGuests();
        room.markDisconnected("guestA", Instant.now().minusSeconds(400));
        room.markReconnected("guestA", Instant.now());

        boolean removedAny = room.removeExpiredDisconnectedPlayers(Instant.now(), Duration.ofSeconds(300));

        assertThat(removedAny).isFalse();
        assertThat(room.findPlayer("guestA")).isPresent();
    }

    @Test
    void aSoloPlayerCanStartAPracticeGameAlone() {
        Room room = Room.create(
                "CODE2", RoomSettings.defaults(GameLanguage.SPANISH), Player.host("host", "t0", "Host"), Instant.now());

        room.start(DEFAULT_PHASE_WORD_SETS, Instant.now());

        assertThat(room.status()).isEqualTo(RoomStatus.IN_PROGRESS);
        assertThat(room.round().turnOrder()).containsExactly("host");
        assertThat(room.round().infiltratorPlayerIds()).isEmpty();
    }

    @Test
    void newPlayersCanJoinAfterTheGameHasStartedButAreNeverTheInfiltrator() {
        Room room = newRoomWithHostAndTwoGuests();
        room.start(DEFAULT_PHASE_WORD_SETS, Instant.now());

        room.addPlayer(Player.guest("lateJoiner", "t3", "LateJoiner"), Instant.now());

        assertThat(room.findPlayer("lateJoiner")).isPresent();
        assertThat(room.round().turnOrder()).doesNotContain("lateJoiner");
        assertThat(room.round().infiltratorPlayerIds()).doesNotContain("lateJoiner");
        assertThat(room.round().targetWordFor("lateJoiner")).isEqualTo("Cama");
    }

    @Test
    void defaultSettingsHaveThreePhasesAndNoInfiltratorForASoloPlayer() {
        Room room = Room.create(
                "CODE3", RoomSettings.defaults(GameLanguage.SPANISH), Player.host("host", "t0", "Host"), Instant.now());

        assertThat(room.settings().phaseCount()).isEqualTo(3);
        assertThat(room.settings().infiltratorCount()).isZero();
    }

    @Test
    void infiltratorCountAutomaticallyBecomesOneThirdOfPlayersAsTheyJoin() {
        Room room = Room.create(
                "CODE4", RoomSettings.defaults(GameLanguage.SPANISH), Player.host("host", "t0", "Host"), Instant.now());

        room.addPlayer(Player.guest("guestA", "t1", "GuestA"), Instant.now());
        assertThat(room.settings().infiltratorCount()).isZero();

        room.addPlayer(Player.guest("guestB", "t2", "GuestB"), Instant.now());
        // 3 players: floor(3/3) = 1 infiltrator, applied automatically
        // without the host ever calling updateSettings.
        assertThat(room.settings().infiltratorCount()).isEqualTo(1);

        room.addPlayer(Player.guest("guestC", "t3", "GuestC"), Instant.now());
        room.addPlayer(Player.guest("guestD", "t4", "GuestD"), Instant.now());
        room.addPlayer(Player.guest("guestE", "t5", "GuestE"), Instant.now());
        // 6 players: floor(6/3) = 2
        assertThat(room.settings().infiltratorCount()).isEqualTo(2);
    }

    @Test
    void hostCustomizingInfiltratorCountStopsItFromAutoTrackingTheHeadcount() {
        Room room = newRoomWithHostAndTwoGuests();
        RoomSettings current = room.settings();
        // 3 players already auto-defaulted to 1; the host explicitly saves
        // 0 instead (a cooperative game despite having enough players).
        room.updateSettings(
                new RoomSettings(current.wordTimeSeconds(), current.voteTimeSeconds(), current.language(), 0,
                        current.phaseCount(), current.daily()),
                Instant.now());

        room.addPlayer(Player.guest("guestC", "t3", "GuestC"), Instant.now());

        assertThat(room.settings().infiltratorCount()).isZero();
    }
}
