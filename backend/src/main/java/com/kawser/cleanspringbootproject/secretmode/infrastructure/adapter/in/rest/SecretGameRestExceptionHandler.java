package com.kawser.cleanspringbootproject.secretmode.infrastructure.adapter.in.rest;

import com.kawser.cleanspringbootproject.secretmode.domain.exception.ConceptNotAvailableException;
import com.kawser.cleanspringbootproject.secretmode.domain.exception.HintNotAvailableException;
import com.kawser.cleanspringbootproject.secretmode.domain.exception.NoUnusedAcceptedWordsForHintException;
import com.kawser.cleanspringbootproject.secretmode.domain.exception.SecretGameDomainException;
import com.kawser.cleanspringbootproject.secretmode.domain.exception.SecretPlayerNotFoundException;
import com.kawser.cleanspringbootproject.secretmode.domain.exception.SecretRoomNotFoundException;
import com.kawser.cleanspringbootproject.secretmode.domain.exception.SecretRoomNotInProgressException;
import com.kawser.cleanspringbootproject.secretmode.infrastructure.adapter.in.rest.dto.SecretGameErrorMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates secretmode domain rule violations into HTTP responses for the
 * REST adapter, mirroring
 * game.infrastructure.adapter.in.rest.GameRestExceptionHandler. Scoped to
 * secretmode's own REST package so it never intercepts the original game
 * module's exceptions (each module maps its own SecretGameDomainException/
 * GameDomainException hierarchy independently).
 */
@RestControllerAdvice(basePackages = "com.kawser.cleanspringbootproject.secretmode.infrastructure.adapter.in.rest")
public class SecretGameRestExceptionHandler {

    @ExceptionHandler({ SecretRoomNotFoundException.class, SecretPlayerNotFoundException.class })
    public ResponseEntity<SecretGameErrorMessage> handleNotFound(SecretGameDomainException ex) {
        return respond(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler({
            SecretRoomNotInProgressException.class,
            HintNotAvailableException.class,
            NoUnusedAcceptedWordsForHintException.class })
    public ResponseEntity<SecretGameErrorMessage> handleConflict(SecretGameDomainException ex) {
        return respond(HttpStatus.CONFLICT, ex);
    }

    @ExceptionHandler(ConceptNotAvailableException.class)
    public ResponseEntity<SecretGameErrorMessage> handleOperationalError(SecretGameDomainException ex) {
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, ex);
    }

    @ExceptionHandler(SecretGameDomainException.class)
    public ResponseEntity<SecretGameErrorMessage> handleGenericDomainException(SecretGameDomainException ex) {
        return respond(HttpStatus.BAD_REQUEST, ex);
    }

    private ResponseEntity<SecretGameErrorMessage> respond(HttpStatus status, SecretGameDomainException ex) {
        return ResponseEntity.status(status).body(new SecretGameErrorMessage(ex.errorCode(), ex.getMessage(), ex.errorArgs()));
    }
}
