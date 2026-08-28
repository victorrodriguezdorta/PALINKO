package com.kawser.cleanspringbootproject.user.application.service;

import com.kawser.cleanspringbootproject.user.application.dto.ChangePasswordCommand;
import com.kawser.cleanspringbootproject.user.application.dto.LoginCommand;
import com.kawser.cleanspringbootproject.user.application.dto.LoginResult;
import com.kawser.cleanspringbootproject.user.application.dto.RequestPasswordResetCommand;
import com.kawser.cleanspringbootproject.user.application.dto.ResendVerificationCommand;
import com.kawser.cleanspringbootproject.user.application.dto.ResetPasswordCommand;
import com.kawser.cleanspringbootproject.user.application.dto.SignupCommand;
import com.kawser.cleanspringbootproject.user.application.dto.UpdateProfileCommand;
import com.kawser.cleanspringbootproject.user.application.dto.UpdateProfileResult;
import com.kawser.cleanspringbootproject.user.application.dto.UserProfileResult;
import com.kawser.cleanspringbootproject.user.application.dto.VerifyAccountCommand;
import com.kawser.cleanspringbootproject.user.application.port.in.ChangePasswordUseCase;
import com.kawser.cleanspringbootproject.user.application.port.in.GetCurrentUserUseCase;
import com.kawser.cleanspringbootproject.user.application.port.in.LoginUseCase;
import com.kawser.cleanspringbootproject.user.application.port.in.RecordDailyCompletionUseCase;
import com.kawser.cleanspringbootproject.user.application.port.in.RequestPasswordResetUseCase;
import com.kawser.cleanspringbootproject.user.application.port.in.ResendVerificationUseCase;
import com.kawser.cleanspringbootproject.user.application.port.in.ResetPasswordUseCase;
import com.kawser.cleanspringbootproject.user.application.port.in.SignupUseCase;
import com.kawser.cleanspringbootproject.user.application.port.in.UpdateProfileUseCase;
import com.kawser.cleanspringbootproject.user.application.port.in.VerifyAccountUseCase;
import com.kawser.cleanspringbootproject.user.application.port.out.AvatarSeedGenerator;
import com.kawser.cleanspringbootproject.user.application.port.out.OtpGenerator;
import com.kawser.cleanspringbootproject.user.application.port.out.PasswordHasher;
import com.kawser.cleanspringbootproject.user.application.port.out.TokenIssuer;
import com.kawser.cleanspringbootproject.user.application.port.out.UserRepository;
import com.kawser.cleanspringbootproject.user.application.port.out.VerificationEmailSender;
import com.kawser.cleanspringbootproject.user.domain.exception.EmailAlreadyExistsException;
import com.kawser.cleanspringbootproject.user.domain.exception.InvalidCredentialsException;
import com.kawser.cleanspringbootproject.user.domain.exception.InvalidCurrentPasswordException;
import com.kawser.cleanspringbootproject.user.domain.exception.InvalidOtpException;
import com.kawser.cleanspringbootproject.user.domain.exception.MissingArgumentsToResetPasswordException;
import com.kawser.cleanspringbootproject.user.domain.exception.PasswordsDoNotMatchException;
import com.kawser.cleanspringbootproject.user.domain.exception.UserAlreadyVerifiedException;
import com.kawser.cleanspringbootproject.user.domain.exception.UserNotEnabledException;
import com.kawser.cleanspringbootproject.user.domain.exception.UserNotFoundException;
import com.kawser.cleanspringbootproject.user.domain.exception.UsernameAlreadyExistsException;
import com.kawser.cleanspringbootproject.user.domain.model.OneTimePassword;
import com.kawser.cleanspringbootproject.user.domain.model.User;
import com.kawser.cleanspringbootproject.user.infrastructure.config.AuthProperties;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Orchestrates every use case for the user module, the same way
 * GameApplicationService does for game — one class implementing all the
 * in-ports since they share the same collaborators.
 */
public class UserApplicationService implements
        SignupUseCase,
        LoginUseCase,
        VerifyAccountUseCase,
        ResendVerificationUseCase,
        RequestPasswordResetUseCase,
        ResetPasswordUseCase,
        GetCurrentUserUseCase,
        UpdateProfileUseCase,
        ChangePasswordUseCase,
        RecordDailyCompletionUseCase {

    private final UserRepository userRepository;
    private final TokenIssuer tokenIssuer;
    private final OtpGenerator otpGenerator;
    private final VerificationEmailSender verificationEmailSender;
    private final AvatarSeedGenerator avatarSeedGenerator;
    private final PasswordHasher passwordHasher;
    private final AuthProperties authProperties;

    public UserApplicationService(
            UserRepository userRepository,
            TokenIssuer tokenIssuer,
            OtpGenerator otpGenerator,
            VerificationEmailSender verificationEmailSender,
            AvatarSeedGenerator avatarSeedGenerator,
            PasswordHasher passwordHasher,
            AuthProperties authProperties) {
        this.userRepository = userRepository;
        this.tokenIssuer = tokenIssuer;
        this.otpGenerator = otpGenerator;
        this.verificationEmailSender = verificationEmailSender;
        this.avatarSeedGenerator = avatarSeedGenerator;
        this.passwordHasher = passwordHasher;
        this.authProperties = authProperties;
    }

    @Override
    public void signup(SignupCommand command) {
        if (userRepository.existsByUsername(command.username())) {
            throw new UsernameAlreadyExistsException(command.username());
        }
        if (userRepository.existsByEmail(command.email())) {
            throw new EmailAlreadyExistsException();
        }

        String passwordHash = passwordHasher.hash(command.password());
        String avatarSeed = command.avatarSeed();
        if (avatarSeed == null || avatarSeed.isBlank()) {
            avatarSeed = avatarSeedGenerator.generate();
        }
        boolean emailVerificationEnabled = authProperties.emailVerificationEnabled();

        if (emailVerificationEnabled) {
            OneTimePassword otp = otpGenerator.generate();
            User user = User.newAccount(command.username(), passwordHash, command.email(), avatarSeed, false, otp);
            userRepository.save(user);
            verificationEmailSender.sendVerificationEmail(command.email(), otp.otp());
        } else {
            User user = User.newAccount(command.username(), passwordHash, command.email(), avatarSeed, true, null);
            userRepository.save(user);
        }
    }

    @Override
    public LoginResult login(LoginCommand command) {
        User user = userRepository.findByUsername(command.username())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordHasher.matches(command.password(), user.passwordHash())) {
            throw new InvalidCredentialsException();
        }
        if (!user.enabled()) {
            throw new UserNotEnabledException();
        }

        String token = tokenIssuer.generateToken(user);
        return new LoginResult(token, toProfile(user));
    }

    @Override
    public void verifyAccount(VerifyAccountCommand command) {
        User user = userRepository.findByEmail(command.email()).orElseThrow(UserNotFoundException::new);

        if (user.enabled()) {
            throw new UserAlreadyVerifiedException();
        }
        if (user.otp() == null || !user.otp().isValid(command.otp(), LocalDateTime.now())) {
            throw new InvalidOtpException();
        }

        userRepository.save(user.verified());
    }

    @Override
    public void resendVerification(ResendVerificationCommand command) {
        User user = userRepository.findByEmail(command.email()).orElseThrow(UserNotFoundException::new);

        if (user.enabled()) {
            throw new UserAlreadyVerifiedException();
        }

        OneTimePassword otp = otpGenerator.generate();
        userRepository.save(user.withOtp(otp));
        verificationEmailSender.sendVerificationEmail(command.email(), otp.otp());
    }

    @Override
    public void requestPasswordReset(RequestPasswordResetCommand command) {
        User user = userRepository.findByEmail(command.email()).orElseThrow(UserNotFoundException::new);

        if (!user.enabled()) {
            throw new UserNotEnabledException();
        }

        OneTimePassword otp = otpGenerator.generate();
        userRepository.save(user.withOtp(otp));
        verificationEmailSender.sendPasswordResetEmail(command.email(), otp.otp());
    }

    @Override
    public void resetPassword(ResetPasswordCommand command) {
        if (command.otp() == null || command.password() == null || command.confirmPassword() == null) {
            throw new MissingArgumentsToResetPasswordException();
        }

        User user = userRepository.findByEmail(command.email()).orElseThrow(UserNotFoundException::new);

        if (user.otp() == null || !user.otp().isValid(command.otp(), LocalDateTime.now())) {
            throw new InvalidOtpException();
        }
        if (!command.password().equals(command.confirmPassword())) {
            throw new PasswordsDoNotMatchException();
        }

        userRepository.save(user.withPasswordHash(passwordHasher.hash(command.password())));
    }

    @Override
    public UserProfileResult getCurrentUser(String username) {
        User user = userRepository.findByUsername(username).orElseThrow(UserNotFoundException::new);
        return toProfile(user);
    }

    @Override
    public UpdateProfileResult updateProfile(UpdateProfileCommand command) {
        User user = userRepository.findByUsername(command.currentUsername()).orElseThrow(UserNotFoundException::new);

        if (command.newUsername() != null && !command.newUsername().equals(user.username())) {
            if (userRepository.existsByUsername(command.newUsername())) {
                throw new UsernameAlreadyExistsException(command.newUsername());
            }
            user = user.withUsername(command.newUsername());
        }
        if (command.newAvatarSeed() != null) {
            user = user.withAvatarSeed(command.newAvatarSeed());
        }

        User saved = userRepository.save(user);
        return new UpdateProfileResult(tokenIssuer.generateToken(saved), toProfile(saved));
    }

    @Override
    public void changePassword(ChangePasswordCommand command) {
        User user = userRepository.findByUsername(command.username()).orElseThrow(UserNotFoundException::new);

        if (!passwordHasher.matches(command.currentPassword(), user.passwordHash())) {
            throw new InvalidCurrentPasswordException();
        }
        if (!command.newPassword().equals(command.confirmNewPassword())) {
            throw new PasswordsDoNotMatchException();
        }

        userRepository.save(user.withPasswordHash(passwordHasher.hash(command.newPassword())));
    }

    @Override
    public void recordDailyCompletion(String username) {
        // Silently ignored rather than thrown: the caller is a just-finished
        // daily room (see GameApplicationService.endWordChain), which has no
        // reasonable way to surface a missing-account error back to the
        // player after the game already ended.
        userRepository.findByUsername(username)
                .ifPresent(user -> userRepository.save(user.withDailyCompletion(LocalDate.now(ZoneOffset.UTC))));
    }

    private UserProfileResult toProfile(User user) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        return new UserProfileResult(
                user.id(), user.username(), user.avatarSeed(), user.role(), user.currentStreakAsOf(today));
    }
}
