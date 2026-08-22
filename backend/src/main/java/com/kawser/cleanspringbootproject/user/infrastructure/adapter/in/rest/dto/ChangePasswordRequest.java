package com.kawser.cleanspringbootproject.user.infrastructure.adapter.in.rest.dto;

import com.kawser.cleanspringbootproject.user.infrastructure.adapter.in.rest.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
        @NotBlank(message = "currentPassword is required") String currentPassword,
        @NotBlank(message = "newPassword is required") @ValidPassword String newPassword,
        @NotBlank(message = "confirmNewPassword is required") String confirmNewPassword) {
}
