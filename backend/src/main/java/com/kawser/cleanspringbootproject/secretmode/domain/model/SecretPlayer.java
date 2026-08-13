package com.kawser.cleanspringbootproject.secretmode.domain.model;

import java.time.Duration;
import java.time.Instant;

/**
 * The single participant in a SecretRoom. Unlike game.domain.model.Player
 * there is no name/avatar/host/score to track here: a solo room has nobody
 * else to show an identity to, and the room itself (not the player) holds
 * the final score once the game ends (see SecretRoom.finalScore).
 */
public class SecretPlayer {

    private final String id;
    private final String reconnectToken;
    private boolean connected;
    private Instant disconnectedAt;

    private SecretPlayer(String id, String reconnectToken) {
        this.id = id;
        this.reconnectToken = reconnectToken;
        this.connected = true;
        this.disconnectedAt = null;
    }

    public static SecretPlayer solo(String id, String reconnectToken) {
        return new SecretPlayer(id, reconnectToken);
    }

    public boolean matchesReconnectToken(String candidateToken) {
        return reconnectToken.equals(candidateToken);
    }

    public void markConnected() {
        this.connected = true;
        this.disconnectedAt = null;
    }

    public void markDisconnected(Instant now) {
        this.connected = false;
        this.disconnectedAt = now;
    }

    public boolean isReconnectWindowExpired(Instant now, Duration reconnectWindow) {
        return disconnectedAt != null && Duration.between(disconnectedAt, now).compareTo(reconnectWindow) > 0;
    }

    public String id() {
        return id;
    }

    public boolean isConnected() {
        return connected;
    }
}
