package com.kawser.cleanspringbootproject.game.infrastructure.adapter.in.rest.dto;

import com.kawser.cleanspringbootproject.game.application.dto.RoomSnapshot;

/**
 * Shared response shape for both "create room" and "join room": the caller
 * needs their own playerId + reconnectToken (never sent again after this),
 * plus the room's current snapshot so the client can render immediately
 * without waiting on the first STOMP push.
 */
public record RoomJoinedResponse(String roomCode, String playerId, String reconnectToken, RoomSnapshot snapshot) {
}
