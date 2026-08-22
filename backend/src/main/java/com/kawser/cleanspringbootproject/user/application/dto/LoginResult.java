package com.kawser.cleanspringbootproject.user.application.dto;

public record LoginResult(String token, UserProfileResult profile) {
}
