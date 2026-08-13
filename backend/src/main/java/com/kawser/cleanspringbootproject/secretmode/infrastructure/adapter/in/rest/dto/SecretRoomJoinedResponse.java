package com.kawser.cleanspringbootproject.secretmode.infrastructure.adapter.in.rest.dto;

import com.kawser.cleanspringbootproject.secretmode.application.dto.SecretRoomSnapshot;

public record SecretRoomJoinedResponse(String roomCode, String playerId, String reconnectToken, SecretRoomSnapshot snapshot) {
}
