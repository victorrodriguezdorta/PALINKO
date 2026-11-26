package com.kawser.cleanspringbootproject.feedback.application.dto;

public record SubmitFeedbackCommand(String username, String suggestion, String issues) {
}
