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

    /**
     * Tells a just-removed player's own client that they were kicked, since
     * notifyRoomUpdated alone can't: it only pushes to players still in
     * room.players(), which the kicked player no longer is. Delivered on
     * the same per-session error channel as any other domain error (see
     * StompRoomNotifier), so the frontend can special-case this one error
     * code to leave the room.
     */
    void notifyPlayerKicked(String roomCode, String kickedPlayerId);
}
