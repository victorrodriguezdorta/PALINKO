package com.kawser.cleanspringbootproject.secretmode.infrastructure.adapter.in.stomp;

import com.kawser.cleanspringbootproject.secretmode.application.dto.GiveUpCommand;
import com.kawser.cleanspringbootproject.secretmode.application.dto.SubmitSecretWordCommand;
import com.kawser.cleanspringbootproject.secretmode.application.dto.SubmitSolutionCommand;
import com.kawser.cleanspringbootproject.secretmode.application.dto.UseHintCommand;
import com.kawser.cleanspringbootproject.secretmode.application.port.in.GiveUpUseCase;
import com.kawser.cleanspringbootproject.secretmode.application.port.in.SubmitSecretWordUseCase;
import com.kawser.cleanspringbootproject.secretmode.application.port.in.SubmitSolutionUseCase;
import com.kawser.cleanspringbootproject.secretmode.application.port.in.UseHintUseCase;
import com.kawser.cleanspringbootproject.secretmode.domain.exception.SecretGameDomainException;
import com.kawser.cleanspringbootproject.secretmode.domain.exception.UnauthenticatedSecretSessionException;
import com.kawser.cleanspringbootproject.secretmode.infrastructure.adapter.in.stomp.SecretStompSessionRegistry.SecretSessionIdentity;
import com.kawser.cleanspringbootproject.secretmode.infrastructure.adapter.in.stomp.dto.SecretStompErrorMessage;
import com.kawser.cleanspringbootproject.secretmode.infrastructure.adapter.in.stomp.dto.SubmitSecretWordMessage;
import com.kawser.cleanspringbootproject.secretmode.infrastructure.adapter.in.stomp.dto.SubmitSolutionMessage;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.util.Map;

/**
 * In-game actions after a client has opened a WebSocket connection for
 * secretmode: submit a word, use a hint, declare a solution, give up.
 * Mirrors game.infrastructure.adapter.in.stomp.GameStompController's shape
 * exactly: every handler mutates state through
 * SecretGameApplicationService and lets SecretRoomNotifier push the
 * player's own snapshot to their private queue; handlers never return a
 * payload directly, except for domain errors, which go to that same caller
 * alone via /user/queue/secret-errors.
 */
@Controller
public class SecretGameStompController {

    private final SubmitSecretWordUseCase submitSecretWordUseCase;
    private final UseHintUseCase useHintUseCase;
    private final SubmitSolutionUseCase submitSolutionUseCase;
    private final GiveUpUseCase giveUpUseCase;
    private final SecretStompSessionRegistry sessionRegistry;

    public SecretGameStompController(
            SubmitSecretWordUseCase submitSecretWordUseCase,
            UseHintUseCase useHintUseCase,
            SubmitSolutionUseCase submitSolutionUseCase,
            GiveUpUseCase giveUpUseCase,
            SecretStompSessionRegistry sessionRegistry) {
        this.submitSecretWordUseCase = submitSecretWordUseCase;
        this.useHintUseCase = useHintUseCase;
        this.submitSolutionUseCase = submitSolutionUseCase;
        this.giveUpUseCase = giveUpUseCase;
        this.sessionRegistry = sessionRegistry;
    }

    @MessageMapping("/secret-rooms/{code}/word")
    public void word(SubmitSecretWordMessage message, SimpMessageHeaderAccessor headerAccessor) {
        SecretSessionIdentity identity = requireIdentity(headerAccessor);
        submitSecretWordUseCase.submitWord(new SubmitSecretWordCommand(
                identity.roomCode(), identity.playerId(), identity.reconnectToken(), message.wordText()));
    }

    @MessageMapping("/secret-rooms/{code}/hint")
    public void hint(SimpMessageHeaderAccessor headerAccessor) {
        SecretSessionIdentity identity = requireIdentity(headerAccessor);
        useHintUseCase.useHint(
                new UseHintCommand(identity.roomCode(), identity.playerId(), identity.reconnectToken()));
    }

    @MessageMapping("/secret-rooms/{code}/solve")
    public void solve(SubmitSolutionMessage message, SimpMessageHeaderAccessor headerAccessor) {
        SecretSessionIdentity identity = requireIdentity(headerAccessor);
        submitSolutionUseCase.submitSolution(new SubmitSolutionCommand(
                identity.roomCode(), identity.playerId(), identity.reconnectToken(), message.formulationText()));
    }

    @MessageMapping("/secret-rooms/{code}/give-up")
    public void giveUp(SimpMessageHeaderAccessor headerAccessor) {
        SecretSessionIdentity identity = requireIdentity(headerAccessor);
        giveUpUseCase.giveUp(
                new GiveUpCommand(identity.roomCode(), identity.playerId(), identity.reconnectToken()));
    }

    @MessageExceptionHandler(SecretGameDomainException.class)
    @SendToUser("/queue/secret-errors")
    public SecretStompErrorMessage handleDomainException(SecretGameDomainException ex) {
        return new SecretStompErrorMessage(ex.errorCode(), ex.getMessage(), ex.errorArgs());
    }

    @MessageExceptionHandler(IllegalArgumentException.class)
    @SendToUser("/queue/secret-errors")
    public SecretStompErrorMessage handleIllegalArgument(IllegalArgumentException ex) {
        return new SecretStompErrorMessage("VALIDATION_ERROR", ex.getMessage(), Map.of());
    }

    private SecretSessionIdentity requireIdentity(SimpMessageHeaderAccessor headerAccessor) {
        return sessionRegistry.find(headerAccessor.getSessionId())
                .orElseThrow(UnauthenticatedSecretSessionException::new);
    }
}
