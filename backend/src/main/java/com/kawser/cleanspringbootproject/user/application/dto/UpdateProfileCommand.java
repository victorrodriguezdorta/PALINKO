package com.kawser.cleanspringbootproject.user.application.dto;

/**
 * Either field may be null to mean "leave unchanged" — a profile edit may
 * touch just the username, just the avatar, or both at once.
 */
public record UpdateProfileCommand(String currentUsername, String newUsername, String newAvatarSeed) {
}
