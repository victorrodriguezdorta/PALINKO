package com.kawser.cleanspringbootproject.secretmode.infrastructure.adapter.in.stomp;

import com.kawser.cleanspringbootproject.secretmode.application.dto.DisconnectSecretPlayerCommand;
import com.kawser.cleanspringbootproject.secretmode.application.port.in.HandleSecretDisconnectUseCase;
import com.kawser.cleanspringbootproject.secretmode.infrastructure.adapter.in.stomp.SecretStompSessionRegistry.SecretSessionIdentity;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * Marks a secret-mode player as disconnected (not removed - they can
 * reconnect with the same reconnectToken later) whenever their WebSocket
 * session drops. Mirrors
 * game.infrastructure.adapter.in.stomp.StompSessionEventListener. Since
 * SecretStompSessionRegistry.find returns empty for a session that never
 * carried secretmode's headers, this is a no-op for original-game sessions.
 */
@Component
public class SecretStompSessionEventListener {

    private final SecretStompSessionRegistry sessionRegistry;
    private final HandleSecretDisconnectUseCase handleSecretDisconnectUseCase;

    public SecretStompSessionEventListener(
            SecretStompSessionRegistry sessionRegistry, HandleSecretDisconnectUseCase handleSecretDisconnectUseCase) {
        this.sessionRegistry = sessionRegistry;
        this.handleSecretDisconnectUseCase = handleSecretDisconnectUseCase;
    }

    @EventListener
    public void onSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        sessionRegistry.find(accessor.getSessionId()).ifPresent(this::handleDisconnect);
        sessionRegistry.remove(accessor.getSessionId());
    }

    private void handleDisconnect(SecretSessionIdentity identity) {
        handleSecretDisconnectUseCase.handleDisconnect(
                new DisconnectSecretPlayerCommand(identity.roomCode(), identity.playerId()));
    }
}
