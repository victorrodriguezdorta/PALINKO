package com.kawser.cleanspringbootproject.user.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record VerifyAccountRequest(
        @NotBlank(message = "email is required") @Email String email,
        @NotBlank(message = "otp is required") String otp) {
}
