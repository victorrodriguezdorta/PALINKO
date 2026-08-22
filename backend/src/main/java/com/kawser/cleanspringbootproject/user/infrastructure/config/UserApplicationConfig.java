package com.kawser.cleanspringbootproject.user.infrastructure.config;

import com.kawser.cleanspringbootproject.user.application.port.out.AvatarSeedGenerator;
import com.kawser.cleanspringbootproject.user.application.port.out.OtpGenerator;
import com.kawser.cleanspringbootproject.user.application.port.out.PasswordHasher;
import com.kawser.cleanspringbootproject.user.application.port.out.TokenIssuer;
import com.kawser.cleanspringbootproject.user.application.port.out.UserRepository;
import com.kawser.cleanspringbootproject.user.application.port.out.VerificationEmailSender;
import com.kawser.cleanspringbootproject.user.application.service.UserApplicationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the framework-agnostic UserApplicationService to its out-port
 * adapters — mirrors game.infrastructure.config.GameApplicationConfig.
 */
@Configuration
public class UserApplicationConfig {

    @Bean
    public UserApplicationService userApplicationService(
            UserRepository userRepository,
            TokenIssuer tokenIssuer,
            OtpGenerator otpGenerator,
            VerificationEmailSender verificationEmailSender,
            AvatarSeedGenerator avatarSeedGenerator,
            PasswordHasher passwordHasher,
            AuthProperties authProperties) {
        return new UserApplicationService(
                userRepository, tokenIssuer, otpGenerator, verificationEmailSender, avatarSeedGenerator,
                passwordHasher, authProperties);
    }
}
