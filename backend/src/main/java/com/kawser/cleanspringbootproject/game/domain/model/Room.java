package com.kawser.cleanspringbootproject.game.domain.model;

import com.kawser.cleanspringbootproject.game.domain.exception.DuplicatePlayerNameException;
import com.kawser.cleanspringbootproject.game.domain.exception.InvalidInfiltratorCountException;
import com.kawser.cleanspringbootproject.game.domain.exception.NotEnoughPlayersException;
import com.kawser.cleanspringbootproject.game.domain.exception.NotHostException;
import com.kawser.cleanspringbootproject.game.domain.exception.PlayerNotFoundException;
import com.kawser.cleanspringbootproject.game.domain.exception.RoomFullException;
import com.kawser.cleanspringbootproject.game.domain.exception.RoomNotFinishedException;
import com.kawser.cleanspringbootproject.game.domain.exception.RoomNotInLobbyException;
import com.kawser.cleanspringbootproject.game.domain.exception.RoomNotInProgressException;
import com.kawser.cleanspringbootproject.game.domain.exception.RoomNotJoinableException;
import com.kawser.cleanspringbootproject.game.domain.exception.RoundNotStartedException;
import com.kawser.cleanspringbootproject.game.domain.service.ScoringPolicy;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Root aggregate for a single game session, held entirely in memory and
 * discarded once the game ends. There is no persistence layer backing this
 * class — every state transition (join, start, reveal, advance) is
 * validated here so the application layer never has to re-derive these
 * invariants.
 */
public class Room {

    public static final int MINIMUM_PLAYERS_TO_START = 1;
    public static final int MAXIMUM_PLAYERS = 10;

    private final String code;
    private final Map<String, Player> players = new LinkedHashMap<>();
    private Round round;
    private RoomSettings settings;
    private final Instant createdAt;
    private String hostPlayerId;
    private RoomStatus status;
    private Instant lastActivityAt;
    // Tracks whether the host has ever set infiltratorCount explicitly (see
    // updateSettings): until they do, it auto-follows maxInfiltratorCount()
    // as players join/leave, so a fresh room always shows the "right"
    // default (a third of the current headcount) without the host having to
    // touch anything.
    private boolean infiltratorCountCustomized = false;

    private Room(String code, RoomSettings settings, Player host, Instant now) {
        this.code = code;
        this.settings = settings;
        this.status = RoomStatus.LOBBY;
        this.createdAt = now;
        this.lastActivityAt = now;
        this.hostPlayerId = host.id();
        this.players.put(host.id(), host);
    }

    public static Room create(String code, RoomSettings settings, Player host, Instant now) {
        return new Room(code, settings, host, now);
    }

    public void addPlayer(Player player, Instant now) {
        // A daily-challenge room is a solo experience by design (see
        // RoomSettings.daily) — it is never shared by a room code, so a
        // second player joining it would just silently break the "everyone
        // gets today's same solo chain" premise rather than serve any real
        // multiplayer use.
        if (settings.daily()) {
            throw new RoomNotJoinableException(code);
        }
        if (status == RoomStatus.FINISHED || status == RoomStatus.CLOSED) {
            throw new RoomNotJoinableException(code);
        }
        if (players.size() >= MAXIMUM_PLAYERS) {
            throw new RoomFullException(code);
        }
        boolean nameTaken = players.values().stream()
                .anyMatch(existing -> existing.name().equalsIgnoreCase(player.name()));
        if (nameTaken) {
            throw new DuplicatePlayerNameException(player.name());
        }
        players.put(player.id(), player);
        reapplyAutomaticInfiltratorCount();
        touch(now);
    }

    /**
     * Lets the host tune round count/timers while still in the lobby.
     * Rejected once the game has started: rounds already scheduled a
     * timeout against the old settings, and mid-game rule changes would
     * make those timers and "rounds remaining" checks inconsistent.
     * infiltratorCount is validated against the room's current headcount
     * here (Room.start clamps it again at start time, since players can
     * still join/leave the lobby after settings are saved). Once the host
     * saves settings, infiltratorCount is considered customized and stops
     * auto-following the headcount (see reapplyAutomaticInfiltratorCount) —
     * even if they saved back the same automatic value.
     */
    public void updateSettings(RoomSettings newSettings, Instant now) {
        if (status != RoomStatus.LOBBY) {
            throw new RoomNotInLobbyException(code);
        }
        if (newSettings.infiltratorCount() > maxInfiltratorCount()) {
            throw new InvalidInfiltratorCountException(newSettings.infiltratorCount(), maxInfiltratorCount());
        }
        this.settings = newSettings;
        this.infiltratorCountCustomized = true;
        touch(now);
    }

    /**
     * Keeps infiltratorCount equal to a third of the current headcount
     * (see maxInfiltratorCount) for as long as the host has never set it
     * explicitly via updateSettings — called after every roster change
     * while still in the lobby, so a fresh room always shows the "right"
     * automatic default (0 below 3 players, otherwise floor(players/3))
     * without the host having to touch anything.
     */
    private void reapplyAutomaticInfiltratorCount() {
        if (infiltratorCountCustomized || status != RoomStatus.LOBBY) {
            return;
        }
        int automaticCount = maxInfiltratorCount();
        if (automaticCount != settings.infiltratorCount()) {
            this.settings = new RoomSettings(
                    settings.wordTimeSeconds(), settings.voteTimeSeconds(), settings.language(),
                    automaticCount, settings.phaseCount(), settings.daily());
        }
    }

    /**
     * Deals out the chain: every player is shuffled into a random turn
     * order, and however many infiltrators the settings call for (clamped
     * to whatever is still valid for the current headcount) are taken from
     * the front of that already-shuffled order — a room with fewer than 3
     * players always clamps down to 0, playing purely cooperatively.
     *
     * @param phaseWordSets every phase's dealt WordSet for the whole game,
     *                      already fully computed (see
     *                      ChainWordBank.fullChain) — its size must equal
     *                      settings.phaseCount().
     */
    public void start(List<WordSet> phaseWordSets, Instant now) {
        if (status != RoomStatus.LOBBY) {
            throw new RoomNotJoinableException(code);
        }
        if (players.size() < MINIMUM_PLAYERS_TO_START) {
            throw new NotEnoughPlayersException(MINIMUM_PLAYERS_TO_START);
        }
        List<String> turnOrder = new ArrayList<>(players.keySet());
        Collections.shuffle(turnOrder);
        int infiltratorCount = Math.min(settings.infiltratorCount(), maxInfiltratorCount());
        Set<String> infiltratorPlayerIds = new HashSet<>(turnOrder.subList(0, infiltratorCount));
        this.round = new Round(phaseWordSets, turnOrder, infiltratorPlayerIds, settings.phaseCount());
        status = RoomStatus.IN_PROGRESS;
        touch(now);
    }

    /**
     * The most infiltrators a room its current size can support: never
     * more than a third of the players, so a majority is always crew.
     */
    private int maxInfiltratorCount() {
        return players.size() / 3;
    }

    /**
     * Reaching REVEAL is the end of the game in this mode — there is no
     * "next round" to advance to, so this also marks the room FINISHED.
     */
    public ChainResult revealRound(ScoringPolicy scoringPolicy, Instant now) {
        requireInProgress();
        ChainResult result = round().reveal(scoringPolicy, players.values());
        status = RoomStatus.FINISHED;
        touch(now);
        return result;
    }

    /**
     * The no-infiltrator path (see Round.finishCooperatively): reaching
     * REVEAL this way is still the end of the game, exactly as with
     * revealRound, just without an accusation to score.
     */
    public ChainResult finishCooperatively(Instant now) {
        requireInProgress();
        ChainResult result = round().finishCooperatively();
        status = RoomStatus.FINISHED;
        touch(now);
        return result;
    }

    /**
     * The instant-loss path (see Round.loseToInfiltrator): a non-infiltrator
     * wrote the infiltrators' secret target word, which ends the game right
     * there as a loss for everyone else, exactly like revealRound/
     * finishCooperatively, just without ever reaching VOTING.
     */
    public ChainResult loseToInfiltrator(Instant now) {
        requireInProgress();
        ChainResult result = round().loseToInfiltrator();
        status = RoomStatus.FINISHED;
        touch(now);
        return result;
    }

    public void awardPoints(String playerId, int delta) {
        requirePlayer(playerId).addScore(delta);
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
        round = null;
        status = RoomStatus.LOBBY;
        reapplyAutomaticInfiltratorCount();
        touch(now);
    }

    public void markDisconnected(String playerId, Instant now) {
        requirePlayer(playerId).markDisconnected(now);
        touch(now);
    }

    /**
     * Purges non-host players whose reconnect grace window has expired —
     * called periodically by the room cleanup sweep, not from any
     * player-facing use case. Safe even mid-round: Round only ever
     * references player ids from the turn order it captured at start, and
     * an expired/removed player's turn simply keeps timing out and being
     * skipped like any other unresponsive player until the chain ends.
     */
    public boolean removeExpiredDisconnectedPlayers(Instant now, Duration reconnectWindow) {
        boolean removedAny =
                players.values().removeIf(player -> !player.isHost() && player.isReconnectWindowExpired(now, reconnectWindow));
        if (removedAny) {
            reapplyAutomaticInfiltratorCount();
        }
        return removedAny;
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

    public Round round() {
        if (round == null) {
            throw new RoundNotStartedException(code);
        }
        return round;
    }

    /**
     * Whether a round has ever been started, regardless of the room's
     * current status. Distinct from checking status != LOBBY: a room can
     * be CLOSED (host left) before ever starting, in which case there is
     * still no round to report in a snapshot.
     */
    public boolean hasRound() {
        return round != null;
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
