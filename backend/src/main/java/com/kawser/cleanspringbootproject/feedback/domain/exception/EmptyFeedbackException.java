package com.kawser.cleanspringbootproject.feedback.domain.exception;

public class EmptyFeedbackException extends FeedbackDomainException {

    public EmptyFeedbackException() {
        super("FEEDBACK_EMPTY", "At least one feedback field must be filled in");
    }
}
