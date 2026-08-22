package com.kawser.cleanspringbootproject.user.infrastructure.adapter.in.rest.dto;

import com.kawser.cleanspringbootproject.user.infrastructure.adapter.in.rest.validation.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PasswordResetDto(
        @NotBlank(message = "email is required") @Email String email,
        @NotBlank(message = "otp is required") String otp,
        @NotBlank(message = "password is required") @ValidPassword String password,
        @NotBlank(message = "confirmPassword is required") String confirmPassword) {
}
