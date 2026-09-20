package com.kawser.cleanspringbootproject.user.infrastructure.adapter.in.rest.dto;

import com.kawser.cleanspringbootproject.user.infrastructure.adapter.in.rest.validation.ValidPassword;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank(message = "username is required") @Size(min = 5, max = 15) String username,
        @NotBlank(message = "email is required") @Email String email,
        @NotBlank(message = "password is required") @ValidPassword String password,
        String avatarSeed) {
}
