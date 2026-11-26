package com.kawser.cleanspringbootproject.feedback.infrastructure.adapter.in.rest.dto;

import java.util.Map;

/**
 * Same {code, message, args} shape as GameErrorMessage/UserErrorMessage so
 * the frontend's ApiError handling works unchanged.
 */
public record FeedbackErrorMessage(String code, String message, Map<String, String> args) {
}
