package com.kawser.cleanspringbootproject.feedback.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.Size;

public record SubmitFeedbackRequest(
        @Size(max = SubmitFeedbackRequest.MAX_LENGTH) String suggestion,
        @Size(max = SubmitFeedbackRequest.MAX_LENGTH) String issues) {

    public static final int MAX_LENGTH = 2000;
}
