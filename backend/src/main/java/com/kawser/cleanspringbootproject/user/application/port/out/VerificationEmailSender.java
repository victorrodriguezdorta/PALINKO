package com.kawser.cleanspringbootproject.user.application.port.out;

public interface VerificationEmailSender {

    void sendVerificationEmail(String toEmail, String otp);

    void sendPasswordResetEmail(String toEmail, String otp);
}
