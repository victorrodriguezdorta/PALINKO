package com.kawser.cleanspringbootproject.user.infrastructure.adapter.out.mail;

import com.kawser.cleanspringbootproject.user.application.port.out.VerificationEmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Active by default (app.auth.email-verification.enabled=false) so local
 * development never needs a working SMTP account — signup skips the email
 * step entirely in that mode (see UserApplicationService.signup), so this
 * sender's methods should never actually be invoked, but it exists so no
 * VerificationEmailSender bean is ever missing.
 */
@Component
@ConditionalOnProperty(name = "app.auth.email-verification.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpVerificationEmailSender implements VerificationEmailSender {

    private static final Logger log = LoggerFactory.getLogger(NoOpVerificationEmailSender.class);

    @Override
    public void sendVerificationEmail(String toEmail, String otp) {
        log.debug("Email verification disabled - skipping verification email to {}", toEmail);
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String otp) {
        log.debug("Email verification disabled - skipping password reset email to {}", toEmail);
    }
}
