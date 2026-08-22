package com.kawser.cleanspringbootproject.user.application.dto;

public record VerifyAccountCommand(String email, String otp) {
}
