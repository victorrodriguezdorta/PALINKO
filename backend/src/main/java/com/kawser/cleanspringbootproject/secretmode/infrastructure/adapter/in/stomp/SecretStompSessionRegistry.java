package com.kawser.cleanspringbootproject.secretmode.infrastructure.adapter.in.stomp;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which secret-mode room/player a live STOMP session belongs to.
 * Independent clone of
 * game.infrastructure.adapter.in.stomp.StompSessionRegistry rather than a
 * shared bean: a physical WebSocket session only ever belongs to one mode
 * at a time (see SecretStompAuthChannelInterceptor, which only registers a
 * session here when its room-code header carries secretmode's "SC-"
 * prefix), so sharing a single table would let one mode's session lookups
 * accidentally shadow the other's.
 */
@Component
public class SecretStompSessionRegistry {

    public record SecretSessionIdentity(String roomCode, String playerId, String reconnectToken) {
    }

    private final Map<String, SecretSessionIdentity> identitiesBySessionId = new ConcurrentHashMap<>();

    public void register(String sessionId, String roomCode, String playerId, String reconnectToken) {
        identitiesBySessionId.put(sessionId, new SecretSessionIdentity(roomCode, playerId, reconnectToken));
    }

    public Optional<SecretSessionIdentity> find(String sessionId) {
        return Optional.ofNullable(identitiesBySessionId.get(sessionId));
    }

    public List<String> sessionIdsFor(String roomCode, String playerId) {
        return identitiesBySessionId.entrySet().stream()
                .filter(entry -> entry.getValue().roomCode().equals(roomCode)
                        && entry.getValue().playerId().equals(playerId))
                .map(Map.Entry::getKey)
                .toList();
    }

    public void remove(String sessionId) {
        identitiesBySessionId.remove(sessionId);
    }
}
