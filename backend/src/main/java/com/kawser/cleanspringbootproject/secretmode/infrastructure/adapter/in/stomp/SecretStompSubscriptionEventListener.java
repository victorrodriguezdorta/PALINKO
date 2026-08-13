package com.kawser.cleanspringbootproject.secretmode.infrastructure.adapter.in.stomp;

import com.kawser.cleanspringbootproject.secretmode.application.dto.ReconnectSecretPlayerCommand;
import com.kawser.cleanspringbootproject.secretmode.application.port.in.ReconnectSecretPlayerUseCase;
import com.kawser.cleanspringbootproject.secretmode.infrastructure.adapter.in.stomp.SecretStompSessionRegistry.SecretSessionIdentity;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

/**
 * Republishes the room's current snapshot once a client's subscription to
 * its private /user/queue/secret-room-updates is actually registered with
 * the broker - same race-avoidance reasoning as
 * game.infrastructure.adapter.in.stomp.StompSubscriptionEventListener's
 * javadoc (the CONNECT-time push races the client's subscribe and is
 * reliably lost, so this re-triggers an idempotent reconnect after the
 * subscription is confirmed).
 */
@Component
public class SecretStompSubscriptionEventListener {

    private static final String ROOM_UPDATES_DESTINATION = "/user/queue/secret-room-updates";

    private final SecretStompSessionRegistry sessionRegistry;
    private final ReconnectSecretPlayerUseCase reconnectSecretPlayerUseCase;

    public SecretStompSubscriptionEventListener(
            SecretStompSessionRegistry sessionRegistry, ReconnectSecretPlayerUseCase reconnectSecretPlayerUseCase) {
        this.sessionRegistry = sessionRegistry;
        this.reconnectSecretPlayerUseCase = reconnectSecretPlayerUseCase;
    }

    @EventListener
    public void onSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();
        if (!ROOM_UPDATES_DESTINATION.equals(destination)) {
            return;
        }
        sessionRegistry.find(accessor.getSessionId()).ifPresent(this::resync);
    }

    private void resync(SecretSessionIdentity identity) {
        reconnectSecretPlayerUseCase.reconnect(
                new ReconnectSecretPlayerCommand(identity.roomCode(), identity.playerId(), identity.reconnectToken()));
    }
}
