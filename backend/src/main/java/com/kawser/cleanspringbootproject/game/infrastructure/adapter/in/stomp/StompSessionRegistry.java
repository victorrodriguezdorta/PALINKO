package com.kawser.cleanspringbootproject.game.infrastructure.adapter.in.stomp;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which room/player a live STOMP session belongs to. Populated by
 * StompAuthChannelInterceptor on CONNECT and consulted by
 * StompSessionEventListener on DISCONNECT — kept as its own component
 * (rather than folded into either) because both need to share the same
 * table.
 */
@Component
public class StompSessionRegistry {

    public record SessionIdentity(String roomCode, String playerId, String reconnectToken) {
    }

    private final Map<String, SessionIdentity> identitiesBySessionId = new ConcurrentHashMap<>();

    public void register(String sessionId, String roomCode, String playerId, String reconnectToken) {
        identitiesBySessionId.put(sessionId, new SessionIdentity(roomCode, playerId, reconnectToken));
    }

    public Optional<SessionIdentity> find(String sessionId) {
        return Optional.ofNullable(identitiesBySessionId.get(sessionId));
    }

    public void remove(String sessionId) {
        identitiesBySessionId.remove(sessionId);
    }
}
