package com.kawser.cleanspringbootproject.user.application.dto;

import com.kawser.cleanspringbootproject.user.domain.model.UserRole;

/**
 * Deliberately excludes email — see the module-wide rule that no response
 * ever exposes an account's email address, even to its own owner.
 */
public record UserProfileResult(Long id, String username, String avatarSeed, UserRole role, int currentStreak) {
}
