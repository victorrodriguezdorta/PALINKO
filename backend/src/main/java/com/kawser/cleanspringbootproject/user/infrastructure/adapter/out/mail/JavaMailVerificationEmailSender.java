package com.kawser.cleanspringbootproject.user.infrastructure.adapter.out.mail;

import com.kawser.cleanspringbootproject.user.application.port.out.VerificationEmailSender;
import com.kawser.cleanspringbootproject.user.infrastructure.config.AuthProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Active only when app.auth.email-verification.enabled=true (see
 * NoOpVerificationEmailSender for the default, debug-friendly path) — links
 * point at app.auth.frontend-base-url rather than a hardcoded host, unlike
 * the reference implementation this module is based on.
 */
@Component
@ConditionalOnProperty(name = "app.auth.email-verification.enabled", havingValue = "true")
public class JavaMailVerificationEmailSender implements VerificationEmailSender {

    private static final Logger log = LoggerFactory.getLogger(JavaMailVerificationEmailSender.class);

    private final JavaMailSender mailSender;
    private final AuthProperties authProperties;

    public JavaMailVerificationEmailSender(JavaMailSender mailSender, AuthProperties authProperties) {
        this.mailSender = mailSender;
        this.authProperties = authProperties;
    }

    @Override
    public void sendVerificationEmail(String toEmail, String otp) {
        String link = authProperties.frontendBaseUrl() + "/verify-account?email=" + encode(toEmail) + "&otp=" + otp;
        send(toEmail, "Verify your PALINKO account",
                "<p>Use this code to verify your account: <b>" + otp + "</b></p>"
                        + "<p>Or click <a href=\"" + link + "\">this link</a>. It expires in 5 minutes.</p>");
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String otp) {
        String link = authProperties.frontendBaseUrl() + "/reset-password?email=" + encode(toEmail) + "&otp=" + otp;
        send(toEmail, "Reset your PALINKO password",
                "<p>Use this code to reset your password: <b>" + otp + "</b></p>"
                        + "<p>Or click <a href=\"" + link + "\">this link</a>. It expires in 5 minutes.</p>");
    }

    private void send(String toEmail, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}", toEmail, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    private static String encode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }
}
