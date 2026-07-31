package com.kawser.cleanspringbootproject.game.domain.model;

import com.kawser.cleanspringbootproject.game.domain.exception.InvalidPlayerNameException;

import java.time.Duration;
import java.time.Instant;

/**
 * A participant in a Room. id is the public identifier shown to every
 * viewer in the room snapshot; reconnectToken is a separate secret handed
 * to the owning client once (over REST) so it can rejoin the same identity
 * after a dropped WebSocket connection without exposing a reusable secret
 * to the other players.
 */
public class Player {

    private static final int MAX_NAME_LENGTH = 24;

    private final String id;
    private final String reconnectToken;
    private final String name;
    private final String avatarSeed;
    private final boolean host;
    private int score;
    private boolean connected;
    private Instant disconnectedAt;
    private boolean kicked;

    private Player(String id, String reconnectToken, String name, String avatarSeed, boolean host) {
        this.id = id;
        this.reconnectToken = reconnectToken;
        this.name = name;
        this.avatarSeed = avatarSeed;
        this.host = host;
        this.score = 0;
        this.connected = true;
        this.disconnectedAt = null;
        this.kicked = false;
    }

    public static Player host(String id, String reconnectToken, String name, String avatarSeed) {
        return new Player(id, reconnectToken, validateName(name), avatarSeed, true);
    }

    public static Player guest(String id, String reconnectToken, String name, String avatarSeed) {
        return new Player(id, reconnectToken, validateName(name), avatarSeed, false);
    }

    private static String validateName(String name) {
        if (name == null) {
            throw new InvalidPlayerNameException("null");
        }
        String trimmed = name.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_NAME_LENGTH) {
            throw new InvalidPlayerNameException(name);
        }
        return trimmed;
    }

    public boolean matchesReconnectToken(String candidateToken) {
        return reconnectToken.equals(candidateToken);
    }

    public void addScore(int delta) {
        this.score += delta;
    }

    public void resetScore() {
        this.score = 0;
    }

    public void markConnected() {
        this.connected = true;
        this.disconnectedAt = null;
    }

    public void markDisconnected(Instant now) {
        this.connected = false;
        this.disconnectedAt = now;
    }

    /**
     * A host-initiated removal, as opposed to a dropped connection: unlike
     * markDisconnected, this is permanent and never eligible for
     * reconnect (see Room.kickPlayer / requireValidToken), regardless of
     * how the room otherwise treats disconnected players.
     */
    public void markKicked(Instant now) {
        this.connected = false;
        this.disconnectedAt = now;
        this.kicked = true;
    }

    public boolean isKicked() {
        return kicked;
    }

    /**
     * Whether this player has been disconnected for longer than the given
     * reconnect grace window — used to purge stale players so a slot (and
     * their name) frees up rather than being held forever by someone who
     * never comes back, while anyone reconnecting inside the window keeps
     * their score and place in the game.
     */
    public boolean isReconnectWindowExpired(Instant now, Duration reconnectWindow) {
        return disconnectedAt != null && Duration.between(disconnectedAt, now).compareTo(reconnectWindow) > 0;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String avatarSeed() {
        return avatarSeed;
    }

    public boolean isHost() {
        return host;
    }

    public int score() {
        return score;
    }

    public boolean isConnected() {
        return connected;
    }
}
