package com.kawser.cleanspringbootproject.game.infrastructure.adapter.in.stomp;

import org.springframework.stereotype.Component;

import java.util.List;
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

    /**
     * Every live session belonging to a given player in a given room —
     * usually one, but a player can briefly hold more than one (e.g. a
     * reloaded tab whose old session hasn't disconnected yet), and
     * RoomNotifier needs to reach all of them so nobody misses an update.
     */
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
