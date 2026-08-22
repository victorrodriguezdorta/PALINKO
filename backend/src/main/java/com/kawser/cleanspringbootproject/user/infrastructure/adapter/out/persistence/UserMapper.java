package com.kawser.cleanspringbootproject.user.infrastructure.adapter.out.persistence;

import com.kawser.cleanspringbootproject.user.domain.model.OneTimePassword;
import com.kawser.cleanspringbootproject.user.domain.model.User;

final class UserMapper {

    private UserMapper() {
    }

    static User toDomain(UserJpaEntity entity) {
        OneTimePassword otp = entity.getOtp() != null && entity.getOtp().getOtp() != null
                ? new OneTimePassword(entity.getOtp().getOtp(), entity.getOtp().getOtpGenerationTime())
                : null;

        return new User(entity.getId(), entity.getUsername(), entity.getPassword(), entity.getEmail(),
                entity.isEnabled(), entity.getRole(), entity.getAvatarSeed(), otp, entity.getCurrentStreak(),
                entity.getLastStreakDate());
    }

    static UserJpaEntity toEntity(User user) {
        OneTimePasswordEmbeddable otp = user.otp() != null
                ? new OneTimePasswordEmbeddable(user.otp().otp(), user.otp().otpGenerationTime())
                : null;

        return new UserJpaEntity(user.id(), user.username(), user.passwordHash(), user.role(), user.email(),
                user.avatarSeed(), user.enabled(), otp, user.currentStreak(), user.lastStreakDate());
    }
}
