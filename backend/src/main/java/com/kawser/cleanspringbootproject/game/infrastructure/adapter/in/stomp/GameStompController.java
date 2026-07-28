package com.kawser.cleanspringbootproject.game.infrastructure.adapter.in.stomp;

import com.kawser.cleanspringbootproject.game.application.dto.AdvanceToNextRoundCommand;
import com.kawser.cleanspringbootproject.game.application.dto.ResetRoomCommand;
import com.kawser.cleanspringbootproject.game.application.dto.RetractAnswerCommand;
import com.kawser.cleanspringbootproject.game.application.dto.StartGameCommand;
import com.kawser.cleanspringbootproject.game.application.dto.SubmitAnswerCommand;
import com.kawser.cleanspringbootproject.game.application.dto.SubmitVoteCommand;
import com.kawser.cleanspringbootproject.game.application.dto.UpdateRoomSettingsCommand;
import com.kawser.cleanspringbootproject.game.application.port.in.AdvanceToNextRoundUseCase;
import com.kawser.cleanspringbootproject.game.application.port.in.ResetRoomUseCase;
import com.kawser.cleanspringbootproject.game.application.port.in.RetractAnswerUseCase;
import com.kawser.cleanspringbootproject.game.application.port.in.StartGameUseCase;
import com.kawser.cleanspringbootproject.game.application.port.in.SubmitAnswerUseCase;
import com.kawser.cleanspringbootproject.game.application.port.in.SubmitVoteUseCase;
import com.kawser.cleanspringbootproject.game.application.port.in.UpdateRoomSettingsUseCase;
import com.kawser.cleanspringbootproject.game.domain.exception.GameDomainException;
import com.kawser.cleanspringbootproject.game.domain.exception.UnauthenticatedSessionException;
import com.kawser.cleanspringbootproject.game.infrastructure.adapter.in.stomp.StompSessionRegistry.SessionIdentity;
import com.kawser.cleanspringbootproject.game.infrastructure.adapter.in.stomp.dto.StompErrorMessage;
import com.kawser.cleanspringbootproject.game.infrastructure.adapter.in.stomp.dto.SubmitAnswerMessage;
import com.kawser.cleanspringbootproject.game.infrastructure.adapter.in.stomp.dto.SubmitVoteMessage;
import com.kawser.cleanspringbootproject.game.infrastructure.adapter.in.stomp.dto.UpdateSettingsMessage;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

/**
 * In-game actions after a client has opened a WebSocket connection: start,
 * answer, vote, next-round. Every handler mutates state through
 * GameApplicationService and lets RoomNotifier push the resulting snapshot
 * to /topic/rooms/{code} — handlers here never return a payload to the
 * caller directly, except for domain errors, which go to that caller alone
 * via /user/queue/errors so a rejected action never disrupts the shared
 * snapshot stream.
 */
@Controller
public class GameStompController {

    private final StartGameUseCase startGameUseCase;
    private final SubmitAnswerUseCase submitAnswerUseCase;
    private final SubmitVoteUseCase submitVoteUseCase;
    private final AdvanceToNextRoundUseCase advanceToNextRoundUseCase;
    private final ResetRoomUseCase resetRoomUseCase;
    private final UpdateRoomSettingsUseCase updateRoomSettingsUseCase;
    private final RetractAnswerUseCase retractAnswerUseCase;
    private final StompSessionRegistry sessionRegistry;

    public GameStompController(
            StartGameUseCase startGameUseCase,
            SubmitAnswerUseCase submitAnswerUseCase,
            SubmitVoteUseCase submitVoteUseCase,
            AdvanceToNextRoundUseCase advanceToNextRoundUseCase,
            ResetRoomUseCase resetRoomUseCase,
            UpdateRoomSettingsUseCase updateRoomSettingsUseCase,
            RetractAnswerUseCase retractAnswerUseCase,
            StompSessionRegistry sessionRegistry) {
        this.startGameUseCase = startGameUseCase;
        this.submitAnswerUseCase = submitAnswerUseCase;
        this.submitVoteUseCase = submitVoteUseCase;
        this.advanceToNextRoundUseCase = advanceToNextRoundUseCase;
        this.resetRoomUseCase = resetRoomUseCase;
        this.updateRoomSettingsUseCase = updateRoomSettingsUseCase;
        this.retractAnswerUseCase = retractAnswerUseCase;
        this.sessionRegistry = sessionRegistry;
    }

    @MessageMapping("/rooms/{code}/start")
    public void start(SimpMessageHeaderAccessor headerAccessor) {
        SessionIdentity identity = requireIdentity(headerAccessor);
        startGameUseCase.startGame(
                new StartGameCommand(identity.roomCode(), identity.playerId(), identity.reconnectToken()));
    }

    @MessageMapping("/rooms/{code}/answer")
    public void answer(SubmitAnswerMessage message, SimpMessageHeaderAccessor headerAccessor) {
        SessionIdentity identity = requireIdentity(headerAccessor);
        submitAnswerUseCase.submitAnswer(new SubmitAnswerCommand(
                identity.roomCode(), identity.playerId(), identity.reconnectToken(), message.answerText()));
    }

    @MessageMapping("/rooms/{code}/cancel-answer")
    public void cancelAnswer(SimpMessageHeaderAccessor headerAccessor) {
        SessionIdentity identity = requireIdentity(headerAccessor);
        retractAnswerUseCase.retractAnswer(
                new RetractAnswerCommand(identity.roomCode(), identity.playerId(), identity.reconnectToken()));
    }

    @MessageMapping("/rooms/{code}/vote")
    public void vote(SubmitVoteMessage message, SimpMessageHeaderAccessor headerAccessor) {
        SessionIdentity identity = requireIdentity(headerAccessor);
        submitVoteUseCase.submitVote(new SubmitVoteCommand(
                identity.roomCode(), identity.playerId(), identity.reconnectToken(), message.votedAnswerId()));
    }

    @MessageMapping("/rooms/{code}/next-round")
    public void nextRound(SimpMessageHeaderAccessor headerAccessor) {
        SessionIdentity identity = requireIdentity(headerAccessor);
        advanceToNextRoundUseCase.advanceToNextRound(
                new AdvanceToNextRoundCommand(identity.roomCode(), identity.playerId(), identity.reconnectToken()));
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
                message.totalRounds(), message.answerTimeSeconds(), message.voteTimeSeconds()));
    }

    @MessageExceptionHandler(GameDomainException.class)
    @SendToUser("/queue/errors")
    public StompErrorMessage handleDomainException(GameDomainException ex) {
        return new StompErrorMessage(ex.getMessage());
    }

    // RoomSettings/Question/Answer/Vote validate themselves via a plain
    // IllegalArgumentException in their constructors (no Bean Validation
    // layer applies to STOMP payloads the way it does for REST DTOs), so
    // this is the only place those invariants would otherwise surface as
    // an unhandled exception instead of a message the host can see.
    @MessageExceptionHandler(IllegalArgumentException.class)
    @SendToUser("/queue/errors")
    public StompErrorMessage handleIllegalArgument(IllegalArgumentException ex) {
        return new StompErrorMessage(ex.getMessage());
    }

    private SessionIdentity requireIdentity(SimpMessageHeaderAccessor headerAccessor) {
        return sessionRegistry.find(headerAccessor.getSessionId())
                .orElseThrow(UnauthenticatedSessionException::new);
    }
}
