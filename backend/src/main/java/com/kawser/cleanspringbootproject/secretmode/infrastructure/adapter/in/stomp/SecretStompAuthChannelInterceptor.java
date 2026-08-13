package com.kawser.cleanspringbootproject.secretmode.infrastructure.adapter.in.stomp;

import com.kawser.cleanspringbootproject.secretmode.application.dto.ReconnectSecretPlayerCommand;
import com.kawser.cleanspringbootproject.secretmode.application.port.in.ReconnectSecretPlayerUseCase;
import com.kawser.cleanspringbootproject.secretmode.infrastructure.adapter.out.persistence.RandomSecretRoomCodeGenerator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

/**
 * Reads the room/player identity off custom STOMP CONNECT headers for
 * secretmode, mirroring
 * game.infrastructure.adapter.in.stomp.StompAuthChannelInterceptor. Added
 * as a second ChannelInterceptor alongside the original game's (see
 * WebSocketConfig) on the same physical /ws endpoint.
 *
 * <p>Only acts when the room-code header carries secretmode's "SC-" prefix
 * (see RandomSecretRoomCodeGenerator): this lets both interceptors sit on
 * the same inbound channel and each cheaply ignore CONNECTs meant for the
 * other mode, without ever having to query the other mode's room
 * repository (which would 404 on a code that simply belongs elsewhere).
 */
@Component
public class SecretStompAuthChannelInterceptor implements ChannelInterceptor {

    public static final String ROOM_CODE_HEADER = "room-code";
    public static final String PLAYER_ID_HEADER = "player-id";
    public static final String RECONNECT_TOKEN_HEADER = "reconnect-token";

    private final SecretStompSessionRegistry sessionRegistry;
    private final ObjectProvider<ReconnectSecretPlayerUseCase> reconnectSecretPlayerUseCaseProvider;

    public SecretStompAuthChannelInterceptor(
            SecretStompSessionRegistry sessionRegistry,
            ObjectProvider<ReconnectSecretPlayerUseCase> reconnectSecretPlayerUseCaseProvider) {
        this.sessionRegistry = sessionRegistry;
        this.reconnectSecretPlayerUseCaseProvider = reconnectSecretPlayerUseCaseProvider;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String roomCode = accessor.getFirstNativeHeader(ROOM_CODE_HEADER);
            String playerId = accessor.getFirstNativeHeader(PLAYER_ID_HEADER);
            String reconnectToken = accessor.getFirstNativeHeader(RECONNECT_TOKEN_HEADER);
            if (roomCode != null && roomCode.startsWith(RandomSecretRoomCodeGenerator.PREFIX)
                    && playerId != null && reconnectToken != null) {
                reconnectSecretPlayerUseCaseProvider.getObject()
                        .reconnect(new ReconnectSecretPlayerCommand(roomCode, playerId, reconnectToken));
                sessionRegistry.register(accessor.getSessionId(), roomCode, playerId, reconnectToken);
            }
        }
        return message;
    }
}
