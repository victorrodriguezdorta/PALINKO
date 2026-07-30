package com.kawser.cleanspringbootproject.game.infrastructure.adapter.in.rest.dto;

import java.util.Map;

/**
 * REST error body for the game module only — kept separate from the
 * shared com.kawser.cleanspringbootproject.exception.message.RestErrorMessage
 * (used by the unrelated CRUD-module exception handling) so this module can
 * carry a stable errorCode/args pair for the frontend to translate, without
 * changing that shared class's shape for every other consumer.
 */
public record GameErrorMessage(String code, String message, Map<String, String> args) {
}
