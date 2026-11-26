package com.kawser.cleanspringbootproject.feedback.domain.exception;

public class FeedbackAlreadySubmittedException extends FeedbackDomainException {

    public FeedbackAlreadySubmittedException() {
        super("FEEDBACK_ALREADY_SUBMITTED", "Feedback already submitted");
    }
}
