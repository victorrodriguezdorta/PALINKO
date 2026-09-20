package com.kawser.cleanspringbootproject.user.infrastructure.adapter.in.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.kawser.cleanspringbootproject.user.application.dto.LoginCommand;
import com.kawser.cleanspringbootproject.user.application.dto.LoginResult;
import com.kawser.cleanspringbootproject.user.application.dto.RequestPasswordResetCommand;
import com.kawser.cleanspringbootproject.user.application.dto.ResendVerificationCommand;
import com.kawser.cleanspringbootproject.user.application.dto.ResetPasswordCommand;
import com.kawser.cleanspringbootproject.user.application.dto.SignupCommand;
import com.kawser.cleanspringbootproject.user.application.dto.VerifyAccountCommand;
import com.kawser.cleanspringbootproject.user.application.port.in.LoginUseCase;
import com.kawser.cleanspringbootproject.user.application.port.in.RequestPasswordResetUseCase;
import com.kawser.cleanspringbootproject.user.application.port.in.ResendVerificationUseCase;
import com.kawser.cleanspringbootproject.user.application.port.in.ResetPasswordUseCase;
import com.kawser.cleanspringbootproject.user.application.port.in.SignupUseCase;
import com.kawser.cleanspringbootproject.user.application.port.in.VerifyAccountUseCase;
import com.kawser.cleanspringbootproject.user.infrastructure.adapter.in.rest.dto.LoginRequest;
import com.kawser.cleanspringbootproject.user.infrastructure.adapter.in.rest.dto.LoginResponse;
import com.kawser.cleanspringbootproject.user.infrastructure.adapter.in.rest.dto.PasswordResetDto;
import com.kawser.cleanspringbootproject.user.infrastructure.adapter.in.rest.dto.PasswordResetRequestDto;
import com.kawser.cleanspringbootproject.user.infrastructure.adapter.in.rest.dto.ResendVerificationRequest;
import com.kawser.cleanspringbootproject.user.infrastructure.adapter.in.rest.dto.SignupRequest;
import com.kawser.cleanspringbootproject.user.infrastructure.adapter.in.rest.dto.UserErrorMessage;
import com.kawser.cleanspringbootproject.user.infrastructure.adapter.in.rest.dto.UserProfileResponse;
import com.kawser.cleanspringbootproject.user.infrastructure.adapter.in.rest.dto.VerifyAccountRequest;

import jakarta.validation.Valid;

/**
 * Signup/login/verification/reset-password entry points. All routes here
 * must stay permitAll in SecurityConfig — see that class for the full list
 * of public routes (this module's plus game's existing ones).
 */
@RestController
public class AuthController {

    private final SignupUseCase signupUseCase;
    private final LoginUseCase loginUseCase;
    private final VerifyAccountUseCase verifyAccountUseCase;
    private final ResendVerificationUseCase resendVerificationUseCase;
    private final RequestPasswordResetUseCase requestPasswordResetUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;

    public AuthController(
            SignupUseCase signupUseCase,
            LoginUseCase loginUseCase,
            VerifyAccountUseCase verifyAccountUseCase,
            ResendVerificationUseCase resendVerificationUseCase,
            RequestPasswordResetUseCase requestPasswordResetUseCase,
            ResetPasswordUseCase resetPasswordUseCase) {
        this.signupUseCase = signupUseCase;
        this.loginUseCase = loginUseCase;
        this.verifyAccountUseCase = verifyAccountUseCase;
        this.resendVerificationUseCase = resendVerificationUseCase;
        this.requestPasswordResetUseCase = requestPasswordResetUseCase;
        this.resetPasswordUseCase = resetPasswordUseCase;
    }

    @PostMapping("/auth/signup")
    public ResponseEntity<Void> signup(@RequestBody @Valid SignupRequest request) {
        signupUseCase.signup(new SignupCommand(
                request.username(), request.email(), request.password(), request.avatarSeed()));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/auth/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        LoginResult result = loginUseCase.login(new LoginCommand(request.username(), request.password()));
        return ResponseEntity.ok(new LoginResponse(result.token(), UserProfileResponse.from(result.profile())));
    }

    @PostMapping("/auth/verify-account")
    public ResponseEntity<Void> verifyAccount(@RequestBody @Valid VerifyAccountRequest request) {
        verifyAccountUseCase.verifyAccount(new VerifyAccountCommand(request.email(), request.otp()));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/auth/resend-verification")
    public ResponseEntity<Void> resendVerification(@RequestBody @Valid ResendVerificationRequest request) {
        resendVerificationUseCase.resendVerification(new ResendVerificationCommand(request.email()));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/password/request-reset")
    public ResponseEntity<Void> requestPasswordReset(@RequestBody @Valid PasswordResetRequestDto request) {
        requestPasswordResetUseCase.requestPasswordReset(new RequestPasswordResetCommand(request.email()));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/password/reset")
    public ResponseEntity<Void> resetPassword(@RequestBody @Valid PasswordResetDto request) {
        resetPasswordUseCase.resetPassword(new ResetPasswordCommand(
                request.email(), request.otp(), request.password(), request.confirmPassword()));
        return ResponseEntity.ok().build();
    }

    // Controller-local @ExceptionHandler: Spring MVC always prefers this
    // over any @ControllerAdvice bean for the same exception type, which is
    // the only mechanism that reliably wins here — see the note on
    // UserRestExceptionHandler for why a @ControllerAdvice-level handler
    // could not be made to take priority over GlobalExceptionsHandler.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<UserErrorMessage> handleValidation(MethodArgumentNotValidException ex) {
        return UserRestExceptionHandler.buildValidationErrorResponse(ex);
    }
}
