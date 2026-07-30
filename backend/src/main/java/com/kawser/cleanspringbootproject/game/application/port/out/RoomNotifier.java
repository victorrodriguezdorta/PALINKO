package com.kawser.cleanspringbootproject.game.application.port.out;

import com.kawser.cleanspringbootproject.game.domain.model.Room;

/**
 * Publishes the current state of a room to every connected client. Takes
 * the domain Room itself (rather than a pre-built RoomSnapshot) because
 * each player must see a personalized snapshot — their own target word,
 * which can secretly differ from everyone else's — so the adapter needs to
 * build one RoomSnapshot per player rather than broadcasting a single
 * shared payload. Kept out of the application layer's direct reach so it
 * stays unaware of STOMP/SimpMessagingTemplate.
 */
public interface RoomNotifier {

    void notifyRoomUpdated(Room room);
}
