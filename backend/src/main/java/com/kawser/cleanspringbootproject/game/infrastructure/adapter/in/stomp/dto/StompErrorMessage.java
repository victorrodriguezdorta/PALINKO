package com.kawser.cleanspringbootproject.game.infrastructure.adapter.in.stomp.dto;

import java.util.Map;

public record StompErrorMessage(String code, String message, Map<String, String> args) {
}
