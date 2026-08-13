package com.kawser.cleanspringbootproject.secretmode.infrastructure.adapter.in.rest;

import com.kawser.cleanspringbootproject.secretmode.application.dto.CreateSecretDailyRoomCommand;
import com.kawser.cleanspringbootproject.secretmode.application.dto.CreateSecretRoomResult;
import com.kawser.cleanspringbootproject.secretmode.application.port.in.CreateSecretDailyRoomUseCase;
import com.kawser.cleanspringbootproject.secretmode.infrastructure.adapter.in.rest.dto.CreateSecretDailyRoomRequest;
import com.kawser.cleanspringbootproject.secretmode.infrastructure.adapter.in.rest.dto.SecretRoomJoinedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Entry point for the one action that happens before a client opens a
 * WebSocket for secretmode: starting the daily challenge. Unlike
 * game.infrastructure.adapter.in.rest.RoomController there is no free
 * "create room"/"join room" pair — secretmode is solo-only for now (see
 * plan notes), so the daily challenge is the sole way in. Everything
 * in-game afterward (word/hint/solve/give-up) goes through STOMP — see
 * SecretGameStompController.
 */
@RestController
@RequestMapping("/secret-rooms")
public class SecretRoomController {

    private final CreateSecretDailyRoomUseCase createSecretDailyRoomUseCase;

    public SecretRoomController(CreateSecretDailyRoomUseCase createSecretDailyRoomUseCase) {
        this.createSecretDailyRoomUseCase = createSecretDailyRoomUseCase;
    }

    @Operation(
            summary = "Start today's secret-mode daily challenge",
            description = "Creates a solo room already in progress, dealt deterministically from today's UTC date")
    @ApiResponse(responseCode = "200", description = "Daily secret-mode room created")
    @PostMapping("/daily")
    public ResponseEntity<SecretRoomJoinedResponse> createDaily(@RequestBody @Valid CreateSecretDailyRoomRequest request) {
        CreateSecretRoomResult result =
                createSecretDailyRoomUseCase.createSecretDailyRoom(new CreateSecretDailyRoomCommand(request.language()));

        return ResponseEntity.ok(new SecretRoomJoinedResponse(
                result.roomCode(), result.playerId(), result.reconnectToken(), result.snapshot()));
    }
}
