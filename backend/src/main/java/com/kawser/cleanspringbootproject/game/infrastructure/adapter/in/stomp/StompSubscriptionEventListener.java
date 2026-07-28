package com.kawser.cleanspringbootproject.game.infrastructure.adapter.in.stomp;

import com.kawser.cleanspringbootproject.game.application.dto.ReconnectCommand;
import com.kawser.cleanspringbootproject.game.application.port.in.ReconnectPlayerUseCase;
import com.kawser.cleanspringbootproject.game.infrastructure.adapter.in.stomp.StompSessionRegistry.SessionIdentity;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

/**
 * Republishes the room's current snapshot once a client's subscription to
 * /topic/rooms/{code} is actually registered with the broker.
 *
 * StompAuthChannelInterceptor already calls ReconnectPlayerUseCase at
 * CONNECT time, which broadcasts a snapshot — but that broadcast races the
 * client's subscribe (which can only happen after it receives the
 * CONNECTED frame back), so it is reliably lost: nobody is subscribed yet
 * when it fires. SessionSubscribeEvent fires after the broker has
 * registered the subscription, so re-triggering the same (idempotent)
 * reconnect here guarantees the client actually receives a snapshot
 * instead of being stuck on "connecting" after a page reload.
 */
@Component
public class StompSubscriptionEventListener {

    private static final String ROOM_TOPIC_PREFIX = "/topic/rooms/";

    private final StompSessionRegistry sessionRegistry;
    private final ReconnectPlayerUseCase reconnectPlayerUseCase;

    public StompSubscriptionEventListener(
            StompSessionRegistry sessionRegistry, ReconnectPlayerUseCase reconnectPlayerUseCase) {
        this.sessionRegistry = sessionRegistry;
        this.reconnectPlayerUseCase = reconnectPlayerUseCase;
    }

    @EventListener
    public void onSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();
        if (destination == null || !destination.startsWith(ROOM_TOPIC_PREFIX)) {
            return;
        }
        sessionRegistry.find(accessor.getSessionId()).ifPresent(this::resync);
    }

    private void resync(SessionIdentity identity) {
        reconnectPlayerUseCase.reconnect(
                new ReconnectCommand(identity.roomCode(), identity.playerId(), identity.reconnectToken()));
    }
}
