package com.kawser.cleanspringbootproject.user.infrastructure.adapter.in.rest.dto;

public record LoginResponse(String token, UserProfileResponse user) {
}
