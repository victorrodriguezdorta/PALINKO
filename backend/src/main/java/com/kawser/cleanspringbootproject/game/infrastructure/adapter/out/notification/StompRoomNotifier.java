package com.kawser.cleanspringbootproject.game.infrastructure.adapter.out.notification;

import com.kawser.cleanspringbootproject.game.application.dto.RoomSnapshot;
import com.kawser.cleanspringbootproject.game.application.port.out.RoomNotifier;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class StompRoomNotifier implements RoomNotifier {

    private final SimpMessagingTemplate messagingTemplate;

    public StompRoomNotifier(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void notifyRoomUpdated(RoomSnapshot snapshot) {
        messagingTemplate.convertAndSend("/topic/rooms/" + snapshot.roomCode(), snapshot);
    }
}
