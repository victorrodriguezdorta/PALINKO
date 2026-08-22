package com.kawser.cleanspringbootproject.user.infrastructure.adapter.in.rest;

import com.kawser.cleanspringbootproject.user.domain.exception.InvalidCredentialsException;
import com.kawser.cleanspringbootproject.user.domain.exception.InvalidCurrentPasswordException;
import com.kawser.cleanspringbootproject.user.domain.exception.InvalidOtpException;
import com.kawser.cleanspringbootproject.user.domain.exception.UserDomainException;
import com.kawser.cleanspringbootproject.user.domain.exception.UserNotFoundException;
import com.kawser.cleanspringbootproject.user.infrastructure.adapter.in.rest.dto.UserErrorMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Translates user domain rule violations into HTTP responses, the same way
 * game.infrastructure.adapter.in.rest.GameRestExceptionHandler does for
 * game.
 *
 * <p>Bean validation failures (MethodArgumentNotValidException) are
 * deliberately NOT handled here, even though this advice is scoped to the
 * user REST package: a @RestControllerAdvice bean cannot be made to
 * out-rank another @ControllerAdvice bean for the same exception type by
 * @Order alone once that other bean already sits at
 * Ordered.HIGHEST_PRECEDENCE (Integer.MIN_VALUE — nothing sorts before it,
 * and "HIGHEST_PRECEDENCE - 1" silently overflows to the *lowest*
 * precedence instead of a usable value), and basePackages scoping does not
 * by itself grant priority over a broader advice. The one mechanism that is
 * guaranteed to win is a controller-local @ExceptionHandler, which Spring
 * MVC always prefers over any @ControllerAdvice — see AuthController's own
 * handleValidation(), which reuses buildValidationErrorResponse() below.
 */
@RestControllerAdvice(basePackages = "com.kawser.cleanspringbootproject.user.infrastructure.adapter.in.rest")
public class UserRestExceptionHandler {

    @ExceptionHandler({ InvalidCredentialsException.class, InvalidOtpException.class, InvalidCurrentPasswordException.class })
    public ResponseEntity<UserErrorMessage> handleUnauthorized(UserDomainException ex) {
        return respond(HttpStatus.UNAUTHORIZED, ex);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<UserErrorMessage> handleNotFound(UserDomainException ex) {
        return respond(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(UserDomainException.class)
    public ResponseEntity<UserErrorMessage> handleDomainException(UserDomainException ex) {
        return respond(HttpStatus.BAD_REQUEST, ex);
    }

    private ResponseEntity<UserErrorMessage> respond(HttpStatus status, UserDomainException ex) {
        return ResponseEntity.status(status).body(new UserErrorMessage(ex.errorCode(), ex.getMessage(), ex.errorArgs()));
    }

    /**
     * Shared by every controller in this module's REST package via a local
     * @ExceptionHandler (see AuthController) so bean validation failures —
     * in particular @ValidPassword, which rebuilds its violation message as
     * "RULE_ONE,RULE_TWO" (see PasswordConstraintValidator) — always render
     * as this module's {code, message, args} shape instead of the generic,
     * un-translatable "{field=message}" string GlobalExceptionsHandler
     * would otherwise produce.
     */
    static ResponseEntity<UserErrorMessage> buildValidationErrorResponse(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        // @ValidPassword-annotated fields are named differently across DTOs
        // ("password" on Signup/PasswordReset, "newPassword" on
        // ChangePassword) but all rebuild their violation message the same
        // way (see PasswordConstraintValidator), so detect by message shape
        // rather than by a single hardcoded field name.
        String passwordRules = errors.values().stream()
                .filter(message -> message != null && message.matches("^[A-Z_]+(,[A-Z_]+)*$"))
                .findFirst()
                .orElse(null);
        if (passwordRules != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new UserErrorMessage("WEAK_PASSWORD", "Password does not meet strength requirements",
                            Map.of("rules", passwordRules)));
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new UserErrorMessage("VALIDATION_ERROR", "Invalid request", errors));
    }
}
