package com.kawser.cleanspringbootproject.user.application.dto;

public record ResetPasswordCommand(String email, String otp, String password, String confirmPassword) {
}
