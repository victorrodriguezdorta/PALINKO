package com.kawser.cleanspringbootproject.game.infrastructure.adapter.out.notification;

import com.kawser.cleanspringbootproject.game.application.dto.RoomSnapshot;
import com.kawser.cleanspringbootproject.game.application.port.out.RoomNotifier;
import com.kawser.cleanspringbootproject.game.domain.model.Player;
import com.kawser.cleanspringbootproject.game.domain.model.Room;
import com.kawser.cleanspringbootproject.game.infrastructure.adapter.in.stomp.StompSessionRegistry;
import com.kawser.cleanspringbootproject.game.infrastructure.adapter.in.stomp.dto.StompErrorMessage;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Builds one personalized RoomSnapshot per player (each sees their own
 * target word, which can secretly differ from everyone else's) and pushes
 * it to that player's own private queue rather than a single shared topic.
 *
 * There is no authenticated Principal backing a STOMP session here, so this
 * cannot rely on the framework's user-destination resolution from a
 * Principal name. Instead it uses Spring's documented fallback for
 * unauthenticated user destinations: pass the session id in place of the
 * username to convertAndSendToUser, using an overload that accepts headers
 * with simpSessionId set. This is also the only option available at all
 * for a push like this one, which happens from outside the handling of any
 * inbound STOMP message (triggered by REST calls, other players' actions,
 * or the PhaseScheduler firing on a background thread) — the framework's
 * automatic "reply to the current message's session" behavior behind
 * @SendToUser (used for /queue/errors) only works as a reply within an
 * inbound message's own handler, which does not apply here.
 */
@Component
public class StompRoomNotifier implements RoomNotifier {

    private static final String DESTINATION = "/queue/room-updates";
    private static final String ERRORS_DESTINATION = "/queue/errors";

    private final SimpMessagingTemplate messagingTemplate;
    private final StompSessionRegistry sessionRegistry;

    public StompRoomNotifier(SimpMessagingTemplate messagingTemplate, StompSessionRegistry sessionRegistry) {
        this.messagingTemplate = messagingTemplate;
        this.sessionRegistry = sessionRegistry;
    }

    @Override
    public void notifyRoomUpdated(Room room) {
        for (Player player : room.players()) {
            RoomSnapshot snapshot = RoomSnapshot.from(room, player.id());
            for (String sessionId : sessionRegistry.sessionIdsFor(room.code(), player.id())) {
                SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
                accessor.setSessionId(sessionId);
                accessor.setLeaveMutable(true);
                messagingTemplate.convertAndSendToUser(sessionId, DESTINATION, snapshot, accessor.getMessageHeaders());
            }
        }
    }

    @Override
    public void notifyPlayerKicked(String roomCode, String kickedPlayerId) {
        StompErrorMessage message = new StompErrorMessage("KICKED", "You were removed from the room by the host", Map.of());
        for (String sessionId : sessionRegistry.sessionIdsFor(roomCode, kickedPlayerId)) {
            SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
            accessor.setSessionId(sessionId);
            accessor.setLeaveMutable(true);
            messagingTemplate.convertAndSendToUser(sessionId, ERRORS_DESTINATION, message, accessor.getMessageHeaders());
        }
    }
}
