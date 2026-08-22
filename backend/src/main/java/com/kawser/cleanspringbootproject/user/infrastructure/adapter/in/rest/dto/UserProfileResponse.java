package com.kawser.cleanspringbootproject.user.infrastructure.adapter.in.rest.dto;

import com.kawser.cleanspringbootproject.user.application.dto.UserProfileResult;
import com.kawser.cleanspringbootproject.user.domain.model.UserRole;

/**
 * Deliberately excludes email — no response in this module ever exposes an
 * account's email address, even to its own owner.
 */
public record UserProfileResponse(Long id, String username, String avatarSeed, UserRole role, int currentStreak) {

    public static UserProfileResponse from(UserProfileResult result) {
        return new UserProfileResponse(
                result.id(), result.username(), result.avatarSeed(), result.role(), result.currentStreak());
    }
}
