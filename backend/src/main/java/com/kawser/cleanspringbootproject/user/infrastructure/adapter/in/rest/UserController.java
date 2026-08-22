package com.kawser.cleanspringbootproject.user.infrastructure.adapter.in.rest;

import com.kawser.cleanspringbootproject.user.application.dto.ChangePasswordCommand;
import com.kawser.cleanspringbootproject.user.application.dto.UpdateProfileCommand;
import com.kawser.cleanspringbootproject.user.application.port.in.ChangePasswordUseCase;
import com.kawser.cleanspringbootproject.user.application.port.in.GetCurrentUserUseCase;
import com.kawser.cleanspringbootproject.user.application.port.in.UpdateProfileUseCase;
import com.kawser.cleanspringbootproject.user.infrastructure.adapter.in.rest.dto.ChangePasswordRequest;
import com.kawser.cleanspringbootproject.user.infrastructure.adapter.in.rest.dto.UpdateProfileRequest;
import com.kawser.cleanspringbootproject.user.infrastructure.adapter.in.rest.dto.UpdateProfileResponse;
import com.kawser.cleanspringbootproject.user.infrastructure.adapter.in.rest.dto.UserErrorMessage;
import com.kawser.cleanspringbootproject.user.infrastructure.adapter.in.rest.dto.UserProfileResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    private final GetCurrentUserUseCase getCurrentUserUseCase;
    private final UpdateProfileUseCase updateProfileUseCase;
    private final ChangePasswordUseCase changePasswordUseCase;

    public UserController(
            GetCurrentUserUseCase getCurrentUserUseCase,
            UpdateProfileUseCase updateProfileUseCase,
            ChangePasswordUseCase changePasswordUseCase) {
        this.getCurrentUserUseCase = getCurrentUserUseCase;
        this.updateProfileUseCase = updateProfileUseCase;
        this.changePasswordUseCase = changePasswordUseCase;
    }

    @GetMapping("/users/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser(Authentication authentication) {
        var profile = getCurrentUserUseCase.getCurrentUser(authentication.getName());
        return ResponseEntity.ok(UserProfileResponse.from(profile));
    }

    @PatchMapping("/users/me")
    public ResponseEntity<UpdateProfileResponse> updateProfile(
            Authentication authentication, @RequestBody @Valid UpdateProfileRequest request) {
        var result = updateProfileUseCase.updateProfile(
                new UpdateProfileCommand(authentication.getName(), request.username(), request.avatarSeed()));
        return ResponseEntity.ok(new UpdateProfileResponse(result.token(), UserProfileResponse.from(result.profile())));
    }

    @PutMapping("/users/me/password")
    public ResponseEntity<Void> changePassword(
            Authentication authentication, @RequestBody @Valid ChangePasswordRequest request) {
        changePasswordUseCase.changePassword(new ChangePasswordCommand(
                authentication.getName(), request.currentPassword(), request.newPassword(),
                request.confirmNewPassword()));
        return ResponseEntity.ok().build();
    }

    // Controller-local @ExceptionHandler: see AuthController's identical
    // handler and UserRestExceptionHandler's note on why a
    // @ControllerAdvice-level handler can't be made to win over
    // GlobalExceptionsHandler for MethodArgumentNotValidException.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<UserErrorMessage> handleValidation(MethodArgumentNotValidException ex) {
        return UserRestExceptionHandler.buildValidationErrorResponse(ex);
    }
}
