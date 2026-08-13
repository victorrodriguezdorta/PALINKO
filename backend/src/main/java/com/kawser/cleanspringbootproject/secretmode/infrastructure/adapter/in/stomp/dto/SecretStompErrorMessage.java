package com.kawser.cleanspringbootproject.secretmode.infrastructure.adapter.in.stomp.dto;

import java.util.Map;

public record SecretStompErrorMessage(String code, String message, Map<String, String> args) {
}
