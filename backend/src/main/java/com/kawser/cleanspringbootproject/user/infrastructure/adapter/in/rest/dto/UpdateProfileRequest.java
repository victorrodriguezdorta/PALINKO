package com.kawser.cleanspringbootproject.user.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.Size;

/**
 * Both fields are optional (null means "leave unchanged") — see
 * UpdateProfileCommand. Size is only enforced when username is actually
 * provided, since @Size on a null value passes validation by default.
 */
public record UpdateProfileRequest(@Size(min = 5, max = 15) String username, String avatarSeed) {
}
