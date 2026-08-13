package com.kawser.cleanspringbootproject.secretmode.application.port.out;

import com.kawser.cleanspringbootproject.secretmode.domain.model.SecretRoom;

/**
 * Publishes the current state of a room to its connected client. There is
 * no notifyPlayerKicked equivalent here (see
 * game.application.port.out.RoomNotifier): a solo secretmode room has no
 * host to kick anyone, so that concern simply doesn't exist yet — a future
 * multiplayer secretmode would add it back if needed.
 */
public interface SecretRoomNotifier {

    void notifyRoomUpdated(SecretRoom room);
}
