package com.kawser.cleanspringbootproject.game.infrastructure.adapter.in.stomp;

import com.kawser.cleanspringbootproject.game.application.dto.KickPlayerCommand;
import com.kawser.cleanspringbootproject.game.application.dto.ResetRoomCommand;
import com.kawser.cleanspringbootproject.game.application.dto.RewindWordCommand;
import com.kawser.cleanspringbootproject.game.application.dto.StartGameCommand;
import com.kawser.cleanspringbootproject.game.application.dto.SubmitVoteCommand;
import com.kawser.cleanspringbootproject.game.application.dto.SubmitWordCommand;
import com.kawser.cleanspringbootproject.game.application.dto.UpdateRoomSettingsCommand;
import com.kawser.cleanspringbootproject.game.application.port.in.KickPlayerUseCase;
import com.kawser.cleanspringbootproject.game.application.port.in.ResetRoomUseCase;
import com.kawser.cleanspringbootproject.game.application.port.in.RewindWordUseCase;
import com.kawser.cleanspringbootproject.game.application.port.in.StartGameUseCase;
import com.kawser.cleanspringbootproject.game.application.port.in.SubmitVoteUseCase;
import com.kawser.cleanspringbootproject.game.application.port.in.SubmitWordUseCase;
import com.kawser.cleanspringbootproject.game.application.port.in.UpdateRoomSettingsUseCase;
import com.kawser.cleanspringbootproject.game.domain.exception.GameDomainException;
import com.kawser.cleanspringbootproject.game.domain.exception.UnauthenticatedSessionException;
import com.kawser.cleanspringbootproject.game.infrastructure.adapter.in.stomp.StompSessionRegistry.SessionIdentity;
import com.kawser.cleanspringbootproject.game.infrastructure.adapter.in.stomp.dto.KickPlayerMessage;
import com.kawser.cleanspringbootproject.game.infrastructure.adapter.in.stomp.dto.StompErrorMessage;
import com.kawser.cleanspringbootproject.game.infrastructure.adapter.in.stomp.dto.SubmitVoteMessage;
import com.kawser.cleanspringbootproject.game.infrastructure.adapter.in.stomp.dto.SubmitWordMessage;
import com.kawser.cleanspringbootproject.game.infrastructure.adapter.in.stomp.dto.TypingBroadcast;
import com.kawser.cleanspringbootproject.game.infrastructure.adapter.in.stomp.dto.TypingMessage;
import com.kawser.cleanspringbootproject.game.infrastructure.adapter.in.stomp.dto.UpdateSettingsMessage;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.util.Map;

/**
 * In-game actions after a client has opened a WebSocket connection: start,
 * submit a word, vote, play-again, update settings, and the ephemeral
 * typing preview. Every handler (except typing) mutates state through
 * GameApplicationService and lets RoomNotifier push each player's own
 * personalized snapshot to their private queue — handlers here never
 * return a payload to the caller directly, except for domain errors, which
 * go to that caller alone via /user/queue/errors so a rejected action
 * never disrupts anyone else's snapshot stream.
 */
@Controller
public class GameStompController {

    private final StartGameUseCase startGameUseCase;
    private final SubmitWordUseCase submitWordUseCase;
    private final SubmitVoteUseCase submitVoteUseCase;
    private final ResetRoomUseCase resetRoomUseCase;
    private final UpdateRoomSettingsUseCase updateRoomSettingsUseCase;
    private final KickPlayerUseCase kickPlayerUseCase;
    private final RewindWordUseCase rewindWordUseCase;
    private final StompSessionRegistry sessionRegistry;
    private final SimpMessagingTemplate messagingTemplate;

    public GameStompController(
            StartGameUseCase startGameUseCase,
            SubmitWordUseCase submitWordUseCase,
            SubmitVoteUseCase submitVoteUseCase,
            ResetRoomUseCase resetRoomUseCase,
            UpdateRoomSettingsUseCase updateRoomSettingsUseCase,
            KickPlayerUseCase kickPlayerUseCase,
            RewindWordUseCase rewindWordUseCase,
            StompSessionRegistry sessionRegistry,
            SimpMessagingTemplate messagingTemplate) {
        this.startGameUseCase = startGameUseCase;
        this.submitWordUseCase = submitWordUseCase;
        this.submitVoteUseCase = submitVoteUseCase;
        this.resetRoomUseCase = resetRoomUseCase;
        this.updateRoomSettingsUseCase = updateRoomSettingsUseCase;
        this.kickPlayerUseCase = kickPlayerUseCase;
        this.rewindWordUseCase = rewindWordUseCase;
        this.sessionRegistry = sessionRegistry;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/rooms/{code}/start")
    public void start(SimpMessageHeaderAccessor headerAccessor) {
        SessionIdentity identity = requireIdentity(headerAccessor);
        startGameUseCase.startGame(
                new StartGameCommand(identity.roomCode(), identity.playerId(), identity.reconnectToken()));
    }

    @MessageMapping("/rooms/{code}/word")
    public void word(SubmitWordMessage message, SimpMessageHeaderAccessor headerAccessor) {
        SessionIdentity identity = requireIdentity(headerAccessor);
        submitWordUseCase.submitWord(new SubmitWordCommand(
                identity.roomCode(), identity.playerId(), identity.reconnectToken(), message.wordText()));
    }

    @MessageMapping("/rooms/{code}/vote")
    public void vote(SubmitVoteMessage message, SimpMessageHeaderAccessor headerAccessor) {
        SessionIdentity identity = requireIdentity(headerAccessor);
        submitVoteUseCase.submitVote(new SubmitVoteCommand(
                identity.roomCode(), identity.playerId(), identity.reconnectToken(), message.suspectPlayerId()));
    }

    @MessageMapping("/rooms/{code}/play-again")
    public void playAgain(SimpMessageHeaderAccessor headerAccessor) {
        SessionIdentity identity = requireIdentity(headerAccessor);
        resetRoomUseCase.resetRoom(
                new ResetRoomCommand(identity.roomCode(), identity.playerId(), identity.reconnectToken()));
    }

    @MessageMapping("/rooms/{code}/update-settings")
    public void updateSettings(UpdateSettingsMessage message, SimpMessageHeaderAccessor headerAccessor) {
        SessionIdentity identity = requireIdentity(headerAccessor);
        updateRoomSettingsUseCase.updateRoomSettings(new UpdateRoomSettingsCommand(
                identity.roomCode(), identity.playerId(), identity.reconnectToken(),
                message.wordTimeSeconds(), message.voteTimeSeconds(), message.language(),
                message.infiltratorCount(), message.phaseCount()));
    }

    @MessageMapping("/rooms/{code}/kick")
    public void kick(KickPlayerMessage message, SimpMessageHeaderAccessor headerAccessor) {
        SessionIdentity identity = requireIdentity(headerAccessor);
        kickPlayerUseCase.kickPlayer(new KickPlayerCommand(
                identity.roomCode(), identity.playerId(), identity.reconnectToken(), message.targetPlayerId()));
    }

    @MessageMapping("/rooms/{code}/rewind")
    public void rewind(SimpMessageHeaderAccessor headerAccessor) {
        SessionIdentity identity = requireIdentity(headerAccessor);
        rewindWordUseCase.rewindWord(
                new RewindWordCommand(identity.roomCode(), identity.playerId(), identity.reconnectToken()));
    }

    /**
     * Ephemeral live-typing preview: relayed straight to the room's shared
     * topic (no secret data involved) without touching
     * GameApplicationService/Room at all, so a keystroke never takes the
     * per-room mutation lock. By design this does not validate that the
     * sender is the current turn player or that the round is even in
     * WORD_CHAIN — the client discards anything that isn't from the
     * player whose turn it currently is, and a stray/late preview from the
     * wrong phase has no effect on game state.
     */
    @MessageMapping("/rooms/{code}/typing")
    public void typing(TypingMessage message, SimpMessageHeaderAccessor headerAccessor) {
        SessionIdentity identity = requireIdentity(headerAccessor);
        messagingTemplate.convertAndSend(
                "/topic/rooms/" + identity.roomCode() + "/typing",
                new TypingBroadcast(identity.playerId(), message.text()));
    }

    @MessageExceptionHandler(GameDomainException.class)
    @SendToUser("/queue/errors")
    public StompErrorMessage handleDomainException(GameDomainException ex) {
        return new StompErrorMessage(ex.errorCode(), ex.getMessage(), ex.errorArgs());
    }

    // RoomSettings/WordSet/Vote validate themselves via a plain
    // IllegalArgumentException in their constructors (no Bean Validation
    // layer applies to STOMP payloads the way it does for REST DTOs), so
    // this is the only place those invariants would otherwise surface as
    // an unhandled exception instead of a message the host can see. These
    // are edge cases from malformed input rather than normal play, so a
    // single generic error code is enough — the frontend shows one generic
    // translated message rather than needing a code per validation rule.
    @MessageExceptionHandler(IllegalArgumentException.class)
    @SendToUser("/queue/errors")
    public StompErrorMessage handleIllegalArgument(IllegalArgumentException ex) {
        return new StompErrorMessage("VALIDATION_ERROR", ex.getMessage(), Map.of());
    }

    private SessionIdentity requireIdentity(SimpMessageHeaderAccessor headerAccessor) {
        return sessionRegistry.find(headerAccessor.getSessionId())
                .orElseThrow(UnauthenticatedSessionException::new);
    }
}
