package com.kawser.cleanspringbootproject.game.domain.model;

import com.kawser.cleanspringbootproject.game.domain.exception.DuplicatePlayerNameException;
import com.kawser.cleanspringbootproject.game.domain.exception.NoRoundsRemainingException;
import com.kawser.cleanspringbootproject.game.domain.exception.NotEnoughPlayersException;
import com.kawser.cleanspringbootproject.game.domain.exception.NotHostException;
import com.kawser.cleanspringbootproject.game.domain.exception.PlayerNotFoundException;
import com.kawser.cleanspringbootproject.game.domain.exception.RoomNotFinishedException;
import com.kawser.cleanspringbootproject.game.domain.exception.RoomNotInLobbyException;
import com.kawser.cleanspringbootproject.game.domain.exception.RoomNotInProgressException;
import com.kawser.cleanspringbootproject.game.domain.exception.RoomNotJoinableException;
import com.kawser.cleanspringbootproject.game.domain.service.ScoringPolicy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Root aggregate for a single game session, held entirely in memory and
 * discarded once the game ends. There is no persistence layer backing this
 * class — every state transition (join, start, reveal, advance) is
 * validated here so the application layer never has to re-derive these
 * invariants.
 */
public class Room {

    public static final int MINIMUM_PLAYERS_TO_START = 2;

    private final String code;
    private final Map<String, Player> players = new LinkedHashMap<>();
    private final List<Round> rounds = new ArrayList<>();
    private RoomSettings settings;
    private final Instant createdAt;
    private String hostPlayerId;
    private RoomStatus status;
    private int currentRoundIndex;
    private Instant lastActivityAt;

    private Room(String code, RoomSettings settings, Player host, Instant now) {
        this.code = code;
        this.settings = settings;
        this.status = RoomStatus.LOBBY;
        this.currentRoundIndex = -1;
        this.createdAt = now;
        this.lastActivityAt = now;
        this.hostPlayerId = host.id();
        this.players.put(host.id(), host);
    }

    public static Room create(String code, RoomSettings settings, Player host, Instant now) {
        return new Room(code, settings, host, now);
    }

    public void addPlayer(Player player, Instant now) {
        if (status == RoomStatus.FINISHED || status == RoomStatus.CLOSED) {
            throw new RoomNotJoinableException(code);
        }
        boolean nameTaken = players.values().stream()
                .anyMatch(existing -> existing.name().equalsIgnoreCase(player.name()));
        if (nameTaken) {
            throw new DuplicatePlayerNameException(player.name());
        }
        players.put(player.id(), player);
        touch(now);
    }

    /**
     * Lets the host tune round count/timers while still in the lobby.
     * Rejected once the game has started: rounds already scheduled a
     * timeout against the old settings, and mid-game rule changes would
     * make those timers and "rounds remaining" checks inconsistent.
     */
    public void updateSettings(RoomSettings newSettings, Instant now) {
        if (status != RoomStatus.LOBBY) {
            throw new RoomNotInLobbyException(code);
        }
        this.settings = newSettings;
        touch(now);
    }

    public void start(Question firstQuestion, Instant now) {
        if (status != RoomStatus.LOBBY) {
            throw new RoomNotJoinableException(code);
        }
        if (players.size() < MINIMUM_PLAYERS_TO_START) {
            throw new NotEnoughPlayersException(MINIMUM_PLAYERS_TO_START);
        }
        rounds.add(new Round(1, firstQuestion));
        currentRoundIndex = 0;
        status = RoomStatus.IN_PROGRESS;
        touch(now);
    }

    public void advanceToNextRoundOrFinish(Question nextQuestion, Instant now) {
        requireInProgress();
        if (currentRoundIndex + 1 >= settings.totalRounds()) {
            status = RoomStatus.FINISHED;
        } else {
            rounds.add(new Round(currentRoundIndex + 2, nextQuestion));
            currentRoundIndex++;
        }
        touch(now);
    }

    public RoundResult revealCurrentRound(ScoringPolicy scoringPolicy, Instant now) {
        requireInProgress();
        Round round = currentRound();
        round.reveal();
        RoundResult result = scoringPolicy.score(round, players.values());
        touch(now);
        return result;
    }

    public boolean hasMoreRounds() {
        return currentRoundIndex + 1 < settings.totalRounds();
    }

    /**
     * Tears the room down because its host left. Idempotent: a room that
     * already finished normally, or was already closed, is left untouched
     * so a late/duplicate disconnect event can never downgrade a completed
     * game's final results back into a "host left" screen.
     */
    public void close(Instant now) {
        if (status == RoomStatus.FINISHED || status == RoomStatus.CLOSED) {
            return;
        }
        status = RoomStatus.CLOSED;
        touch(now);
    }

    /**
     * Resets a finished room back to its initial LOBBY state so the same
     * group can play again without recreating the room or handing out a
     * new code: rounds and scores are cleared, and any player no longer
     * connected is dropped (only whoever is still around when the host
     * chooses to play again carries over). Room settings (round count,
     * timers) are untouched, exactly as they were when the room was first
     * created. Idempotent from LOBBY itself so a duplicate click is a
     * no-op rather than an error.
     */
    public void resetToLobby(Instant now) {
        if (status == RoomStatus.LOBBY) {
            return;
        }
        if (status != RoomStatus.FINISHED) {
            throw new RoomNotFinishedException(code);
        }
        players.values().removeIf(player -> !player.isHost() && !player.isConnected());
        players.values().forEach(Player::resetScore);
        rounds.clear();
        currentRoundIndex = -1;
        status = RoomStatus.LOBBY;
        touch(now);
    }

    public void markDisconnected(String playerId, Instant now) {
        requirePlayer(playerId).markDisconnected();
        touch(now);
    }

    public void markReconnected(String playerId, Instant now) {
        requirePlayer(playerId).markConnected();
        touch(now);
    }

    public void requireHost(String playerId) {
        if (!hostPlayerId.equals(playerId)) {
            throw new NotHostException(playerId);
        }
    }

    public int connectedHumanPlayerCount() {
        return (int) players.values().stream().filter(Player::isConnected).count();
    }

    private void requireInProgress() {
        if (status != RoomStatus.IN_PROGRESS) {
            throw new RoomNotInProgressException(code);
        }
    }

    private Player requirePlayer(String playerId) {
        Player player = players.get(playerId);
        if (player == null) {
            throw new PlayerNotFoundException(playerId);
        }
        return player;
    }

    private void touch(Instant now) {
        this.lastActivityAt = now;
    }

    public Round currentRound() {
        if (currentRoundIndex < 0 || currentRoundIndex >= rounds.size()) {
            throw new NoRoundsRemainingException(code);
        }
        return rounds.get(currentRoundIndex);
    }

    /**
     * Whether a round has ever been started, regardless of the room's
     * current status. Distinct from checking status != LOBBY: a room can
     * be CLOSED (host left) before ever starting, in which case there is
     * still no round to report in a snapshot.
     */
    public boolean hasCurrentRound() {
        return currentRoundIndex >= 0 && currentRoundIndex < rounds.size();
    }

    public Optional<Player> findPlayer(String playerId) {
        return Optional.ofNullable(players.get(playerId));
    }

    public boolean isHost(String playerId) {
        return hostPlayerId.equals(playerId);
    }

    public String code() {
        return code;
    }

    public String hostPlayerId() {
        return hostPlayerId;
    }

    public Collection<Player> players() {
        return List.copyOf(players.values());
    }

    public RoomStatus status() {
        return status;
    }

    public RoomSettings settings() {
        return settings;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant lastActivityAt() {
        return lastActivityAt;
    }
}
