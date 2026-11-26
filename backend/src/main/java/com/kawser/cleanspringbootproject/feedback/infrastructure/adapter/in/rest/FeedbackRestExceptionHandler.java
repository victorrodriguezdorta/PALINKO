package com.kawser.cleanspringbootproject.feedback.infrastructure.adapter.in.rest;

import com.kawser.cleanspringbootproject.feedback.domain.exception.FeedbackAlreadySubmittedException;
import com.kawser.cleanspringbootproject.feedback.domain.exception.FeedbackDomainException;
import com.kawser.cleanspringbootproject.feedback.infrastructure.adapter.in.rest.dto.FeedbackErrorMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.kawser.cleanspringbootproject.feedback.infrastructure.adapter.in.rest")
public class FeedbackRestExceptionHandler {

    @ExceptionHandler(FeedbackAlreadySubmittedException.class)
    public ResponseEntity<FeedbackErrorMessage> handleConflict(FeedbackDomainException ex) {
        return respond(HttpStatus.CONFLICT, ex);
    }

    @ExceptionHandler(FeedbackDomainException.class)
    public ResponseEntity<FeedbackErrorMessage> handleDomainException(FeedbackDomainException ex) {
        return respond(HttpStatus.BAD_REQUEST, ex);
    }

    private ResponseEntity<FeedbackErrorMessage> respond(HttpStatus status, FeedbackDomainException ex) {
        return ResponseEntity.status(status)
                .body(new FeedbackErrorMessage(ex.errorCode(), ex.getMessage(), ex.errorArgs()));
    }
}
