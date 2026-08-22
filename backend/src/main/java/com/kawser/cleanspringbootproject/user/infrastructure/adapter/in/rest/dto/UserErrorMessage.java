package com.kawser.cleanspringbootproject.user.infrastructure.adapter.in.rest.dto;

import java.util.Map;

/**
 * REST error body for the user module only — mirrors game's
 * GameErrorMessage so the frontend's translateError()/ApiError handling
 * works identically for both modules.
 */
public record UserErrorMessage(String code, String message, Map<String, String> args) {
}
