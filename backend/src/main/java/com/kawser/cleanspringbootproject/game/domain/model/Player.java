package com.kawser.cleanspringbootproject.game.domain.model;

import com.kawser.cleanspringbootproject.game.domain.exception.InvalidPlayerNameException;

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
    private final boolean host;
    private int score;
    private boolean connected;

    private Player(String id, String reconnectToken, String name, boolean host) {
        this.id = id;
        this.reconnectToken = reconnectToken;
        this.name = name;
        this.host = host;
        this.score = 0;
        this.connected = true;
    }

    public static Player host(String id, String reconnectToken, String name) {
        return new Player(id, reconnectToken, validateName(name), true);
    }

    public static Player guest(String id, String reconnectToken, String name) {
        return new Player(id, reconnectToken, validateName(name), false);
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
    }

    public void markDisconnected() {
        this.connected = false;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
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
