package com.kawser.cleanspringbootproject.game.infrastructure.adapter.in.stomp;

import com.kawser.cleanspringbootproject.game.application.dto.ReconnectCommand;
import com.kawser.cleanspringbootproject.game.application.port.in.ReconnectPlayerUseCase;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

/**
 * Reads the room/player identity off custom STOMP CONNECT headers, records
 * it in StompSessionRegistry (so @MessageMapping handlers and the
 * disconnect listener don't need the client to repeat the reconnectToken in
 * every message payload), and calls ReconnectPlayerUseCase so the CONNECT
 * doubles as re-authentication: an invalid/stale token rejects the
 * handshake instead of silently registering a session, and a genuine
 * reconnect after a drop flips Player.connected back to true — without
 * this call the flag would stay false forever after any disconnect.
 *
 * ReconnectPlayerUseCase is looked up lazily through an ObjectProvider
 * rather than injected directly: GameApplicationService (which implements
 * it) depends on RoomNotifier -> SimpMessagingTemplate -> Spring's
 * WebSocket message broker wiring -> WebSocketConfig -> this interceptor,
 * so a direct constructor dependency here closes that cycle. Neither side
 * needs the other before a real STOMP CONNECT happens, so deferring the
 * lookup to call time breaks the cycle without changing behavior.
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    public static final String ROOM_CODE_HEADER = "room-code";
    public static final String PLAYER_ID_HEADER = "player-id";
    public static final String RECONNECT_TOKEN_HEADER = "reconnect-token";

    private final StompSessionRegistry sessionRegistry;
    private final ObjectProvider<ReconnectPlayerUseCase> reconnectPlayerUseCaseProvider;

    public StompAuthChannelInterceptor(
            StompSessionRegistry sessionRegistry,
            ObjectProvider<ReconnectPlayerUseCase> reconnectPlayerUseCaseProvider) {
        this.sessionRegistry = sessionRegistry;
        this.reconnectPlayerUseCaseProvider = reconnectPlayerUseCaseProvider;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String roomCode = accessor.getFirstNativeHeader(ROOM_CODE_HEADER);
            String playerId = accessor.getFirstNativeHeader(PLAYER_ID_HEADER);
            String reconnectToken = accessor.getFirstNativeHeader(RECONNECT_TOKEN_HEADER);
            if (roomCode != null && playerId != null && reconnectToken != null) {
                reconnectPlayerUseCaseProvider.getObject()
                        .reconnect(new ReconnectCommand(roomCode, playerId, reconnectToken));
                sessionRegistry.register(accessor.getSessionId(), roomCode, playerId, reconnectToken);
            }
        }
        return message;
    }
}
