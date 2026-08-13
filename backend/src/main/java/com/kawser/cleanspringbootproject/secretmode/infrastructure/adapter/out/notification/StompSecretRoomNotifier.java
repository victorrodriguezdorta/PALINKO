package com.kawser.cleanspringbootproject.secretmode.infrastructure.adapter.out.notification;

import com.kawser.cleanspringbootproject.secretmode.application.dto.SecretRoomSnapshot;
import com.kawser.cleanspringbootproject.secretmode.application.port.out.SecretRoomNotifier;
import com.kawser.cleanspringbootproject.secretmode.domain.model.SecretRoom;
import com.kawser.cleanspringbootproject.secretmode.infrastructure.adapter.in.stomp.SecretStompSessionRegistry;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Pushes the room's current snapshot to its player's private queue.
 * Mirrors game.infrastructure.adapter.out.notification.StompRoomNotifier's
 * reasoning exactly: there is no authenticated Principal backing a STOMP
 * session here, so this uses Spring's documented fallback of passing the
 * session id in place of the username to convertAndSendToUser via headers
 * carrying simpSessionId - the only option available for a push that
 * happens outside the handling of any inbound STOMP message (triggered by
 * the REST create-daily-room call, or PhaseScheduler-equivalent background
 * work, if secretmode ever grows one).
 */
@Component
public class StompSecretRoomNotifier implements SecretRoomNotifier {

    private static final String DESTINATION = "/queue/secret-room-updates";

    private final SimpMessagingTemplate messagingTemplate;
    private final SecretStompSessionRegistry sessionRegistry;

    public StompSecretRoomNotifier(SimpMessagingTemplate messagingTemplate, SecretStompSessionRegistry sessionRegistry) {
        this.messagingTemplate = messagingTemplate;
        this.sessionRegistry = sessionRegistry;
    }

    @Override
    public void notifyRoomUpdated(SecretRoom room) {
        SecretRoomSnapshot snapshot = SecretRoomSnapshot.from(room);
        for (String sessionId : sessionRegistry.sessionIdsFor(room.code(), room.player().id())) {
            SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
            accessor.setSessionId(sessionId);
            accessor.setLeaveMutable(true);
            messagingTemplate.convertAndSendToUser(sessionId, DESTINATION, snapshot, accessor.getMessageHeaders());
        }
    }
}
