package com.kawser.cleanspringbootproject.user.application.dto;

public record ChangePasswordCommand(String username, String currentPassword, String newPassword, String confirmNewPassword) {
}
