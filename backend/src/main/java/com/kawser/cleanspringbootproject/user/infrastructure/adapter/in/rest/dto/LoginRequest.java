package com.kawser.cleanspringbootproject.user.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "username is required") String username,
        @NotBlank(message = "password is required") String password) {
}
