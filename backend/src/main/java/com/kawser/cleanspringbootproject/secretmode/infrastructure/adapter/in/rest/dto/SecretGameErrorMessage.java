package com.kawser.cleanspringbootproject.secretmode.infrastructure.adapter.in.rest.dto;

import java.util.Map;

public record SecretGameErrorMessage(String code, String message, Map<String, String> args) {
}
