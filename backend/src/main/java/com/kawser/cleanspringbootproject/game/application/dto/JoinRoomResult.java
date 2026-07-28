package com.kawser.cleanspringbootproject.game.application.dto;

public record JoinRoomResult(String playerId, String reconnectToken, RoomSnapshot snapshot) {
}
