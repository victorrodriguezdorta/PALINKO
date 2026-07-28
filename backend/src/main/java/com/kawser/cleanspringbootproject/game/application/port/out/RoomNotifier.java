package com.kawser.cleanspringbootproject.game.application.port.out;

import com.kawser.cleanspringbootproject.game.application.dto.RoomSnapshot;

/**
 * Publishes the current state of a room to every connected client. Kept out
 * of the application layer's direct reach so it stays unaware of STOMP/
 * SimpMessagingTemplate.
 */
public interface RoomNotifier {

    void notifyRoomUpdated(RoomSnapshot snapshot);
}
