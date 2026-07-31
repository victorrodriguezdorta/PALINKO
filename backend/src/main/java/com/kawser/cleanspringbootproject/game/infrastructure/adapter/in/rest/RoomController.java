package com.kawser.cleanspringbootproject.game.infrastructure.adapter.in.rest;

import com.kawser.cleanspringbootproject.game.application.dto.CreateDailyRoomCommand;
import com.kawser.cleanspringbootproject.game.application.dto.CreateRoomCommand;
import com.kawser.cleanspringbootproject.game.application.dto.CreateRoomResult;
import com.kawser.cleanspringbootproject.game.application.dto.JoinRoomCommand;
import com.kawser.cleanspringbootproject.game.application.dto.JoinRoomResult;
import com.kawser.cleanspringbootproject.game.application.port.in.CreateDailyRoomUseCase;
import com.kawser.cleanspringbootproject.game.application.port.in.CreateRoomUseCase;
import com.kawser.cleanspringbootproject.game.application.port.in.JoinRoomUseCase;
import com.kawser.cleanspringbootproject.game.infrastructure.adapter.in.rest.dto.CreateDailyRoomRequest;
import com.kawser.cleanspringbootproject.game.infrastructure.adapter.in.rest.dto.CreateRoomRequest;
import com.kawser.cleanspringbootproject.game.infrastructure.adapter.in.rest.dto.JoinRoomRequest;
import com.kawser.cleanspringbootproject.game.infrastructure.adapter.in.rest.dto.RoomJoinedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Entry point for the two actions that happen before a client opens a
 * WebSocket: creating a room and joining one with just a name. Everything
 * in-game afterward (start/answer/vote/next-round) goes through STOMP —
 * see GameStompController.
 */
@RestController
@RequestMapping("/rooms")
public class RoomController {

    private final CreateRoomUseCase createRoomUseCase;
    private final CreateDailyRoomUseCase createDailyRoomUseCase;
    private final JoinRoomUseCase joinRoomUseCase;

    public RoomController(
            CreateRoomUseCase createRoomUseCase,
            CreateDailyRoomUseCase createDailyRoomUseCase,
            JoinRoomUseCase joinRoomUseCase) {
        this.createRoomUseCase = createRoomUseCase;
        this.createDailyRoomUseCase = createDailyRoomUseCase;
        this.joinRoomUseCase = joinRoomUseCase;
    }

    @Operation(summary = "Create a new room", description = "Creates a room in LOBBY status and returns the host's identity")
    @ApiResponse(responseCode = "200", description = "Room created")
    @PostMapping
    public ResponseEntity<RoomJoinedResponse> create(@RequestBody @Valid CreateRoomRequest request) {
        CreateRoomResult result = createRoomUseCase.createRoom(
                new CreateRoomCommand(request.hostName(), request.avatarSeed(), request.language()));

        return ResponseEntity.ok(new RoomJoinedResponse(
                result.roomCode(), result.playerId(), result.reconnectToken(), result.snapshot()));
    }

    @Operation(
            summary = "Start today's daily challenge",
            description = "Creates a solo room already in progress, dealt deterministically from today's UTC date "
                    + "so every player gets the same phases")
    @ApiResponse(responseCode = "200", description = "Daily challenge room created")
    @PostMapping("/daily")
    public ResponseEntity<RoomJoinedResponse> createDaily(@RequestBody @Valid CreateDailyRoomRequest request) {
        CreateRoomResult result = createDailyRoomUseCase.createDailyRoom(new CreateDailyRoomCommand(request.language()));

        return ResponseEntity.ok(new RoomJoinedResponse(
                result.roomCode(), result.playerId(), result.reconnectToken(), result.snapshot()));
    }

    @Operation(summary = "Join a room", description = "Joins an existing room in LOBBY status with just a display name")
    @ApiResponse(responseCode = "200", description = "Joined the room")
    @PostMapping("/{code}/join")
    public ResponseEntity<RoomJoinedResponse> join(
            @PathVariable("code") String code,
            @RequestBody @Valid JoinRoomRequest request) {
        JoinRoomResult result =
                joinRoomUseCase.joinRoom(new JoinRoomCommand(code, request.playerName(), request.avatarSeed()));

        return ResponseEntity.ok(new RoomJoinedResponse(
                code, result.playerId(), result.reconnectToken(), result.snapshot()));
    }
}
