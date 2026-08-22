package com.kawser.cleanspringbootproject.user.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.LocalDateTime;

/**
 * JPA-mapped mirror of the domain OneTimePassword record — kept separate so
 * the domain model never carries persistence annotations.
 */
@Embeddable
public class OneTimePasswordEmbeddable {

    @Column(name = "otp")
    private String otp;

    @Column(name = "otp_generation_time")
    private LocalDateTime otpGenerationTime;

    protected OneTimePasswordEmbeddable() {
    }

    public OneTimePasswordEmbeddable(String otp, LocalDateTime otpGenerationTime) {
        this.otp = otp;
        this.otpGenerationTime = otpGenerationTime;
    }

    public String getOtp() {
        return otp;
    }

    public LocalDateTime getOtpGenerationTime() {
        return otpGenerationTime;
    }
}
