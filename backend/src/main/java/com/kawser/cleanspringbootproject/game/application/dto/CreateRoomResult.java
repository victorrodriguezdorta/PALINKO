package com.kawser.cleanspringbootproject.game.application.dto;

public record CreateRoomResult(String roomCode, String playerId, String reconnectToken, RoomSnapshot snapshot) {
}
