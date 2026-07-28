package com.kawser.cleanspringbootproject.game.infrastructure.adapter.in.rest;

import com.kawser.cleanspringbootproject.exception.message.RestErrorMessage;
import com.kawser.cleanspringbootproject.game.domain.exception.DuplicatePlayerNameException;
import com.kawser.cleanspringbootproject.game.domain.exception.GameDomainException;
import com.kawser.cleanspringbootproject.game.domain.exception.InvalidPlayerNameException;
import com.kawser.cleanspringbootproject.game.domain.exception.NotEnoughPlayersException;
import com.kawser.cleanspringbootproject.game.domain.exception.NotHostException;
import com.kawser.cleanspringbootproject.game.domain.exception.PlayerNotFoundException;
import com.kawser.cleanspringbootproject.game.domain.exception.RoomNotFoundException;
import com.kawser.cleanspringbootproject.game.domain.exception.RoomNotJoinableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates game domain rule violations into HTTP responses for the REST
 * adapter (create/join room). STOMP has its own error channel — see
 * GameStompController — since a per-user message can't be represented as
 * an HTTP status.
 */
@RestControllerAdvice(basePackages = "com.kawser.cleanspringbootproject.game.infrastructure.adapter.in.rest")
public class GameRestExceptionHandler {

    @ExceptionHandler(RoomNotFoundException.class)
    public ResponseEntity<RestErrorMessage> handleRoomNotFound(RoomNotFoundException ex) {
        return respond(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(PlayerNotFoundException.class)
    public ResponseEntity<RestErrorMessage> handlePlayerNotFound(PlayerNotFoundException ex) {
        return respond(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler({ RoomNotJoinableException.class, DuplicatePlayerNameException.class })
    public ResponseEntity<RestErrorMessage> handleConflict(GameDomainException ex) {
        return respond(HttpStatus.CONFLICT, ex);
    }

    @ExceptionHandler({ InvalidPlayerNameException.class, NotEnoughPlayersException.class })
    public ResponseEntity<RestErrorMessage> handleBadRequest(GameDomainException ex) {
        return respond(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(NotHostException.class)
    public ResponseEntity<RestErrorMessage> handleForbidden(NotHostException ex) {
        return respond(HttpStatus.FORBIDDEN, ex);
    }

    @ExceptionHandler(GameDomainException.class)
    public ResponseEntity<RestErrorMessage> handleGenericDomainException(GameDomainException ex) {
        return respond(HttpStatus.BAD_REQUEST, ex);
    }

    private ResponseEntity<RestErrorMessage> respond(HttpStatus status, GameDomainException ex) {
        return ResponseEntity.status(status).body(new RestErrorMessage(status, ex.getMessage()));
    }
}
