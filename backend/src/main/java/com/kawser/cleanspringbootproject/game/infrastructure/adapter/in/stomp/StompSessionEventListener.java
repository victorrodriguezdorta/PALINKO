package com.kawser.cleanspringbootproject.game.infrastructure.adapter.in.stomp;

import com.kawser.cleanspringbootproject.game.application.dto.DisconnectCommand;
import com.kawser.cleanspringbootproject.game.application.port.in.HandleDisconnectUseCase;
import com.kawser.cleanspringbootproject.game.infrastructure.adapter.in.stomp.StompSessionRegistry.SessionIdentity;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * Marks a player as disconnected (not removed — they can reconnect with the
 * same reconnectToken later) whenever their WebSocket session drops for any
 * reason.
 */
@Component
public class StompSessionEventListener {

    private final StompSessionRegistry sessionRegistry;
    private final HandleDisconnectUseCase handleDisconnectUseCase;

    public StompSessionEventListener(StompSessionRegistry sessionRegistry, HandleDisconnectUseCase handleDisconnectUseCase) {
        this.sessionRegistry = sessionRegistry;
        this.handleDisconnectUseCase = handleDisconnectUseCase;
    }

    @EventListener
    public void onSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        sessionRegistry.find(accessor.getSessionId()).ifPresent(this::handleDisconnect);
        sessionRegistry.remove(accessor.getSessionId());
    }

    private void handleDisconnect(SessionIdentity identity) {
        handleDisconnectUseCase.handleDisconnect(new DisconnectCommand(identity.roomCode(), identity.playerId()));
    }
}
