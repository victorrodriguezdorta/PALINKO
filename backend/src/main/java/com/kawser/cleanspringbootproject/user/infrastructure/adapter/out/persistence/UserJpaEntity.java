package com.kawser.cleanspringbootproject.user.infrastructure.adapter.out.persistence;

import com.kawser.cleanspringbootproject.user.domain.model.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * The only class in the user module with JPA annotations — mapped from/to
 * the pure domain User by UserMapper. role is persisted with
 * @Enumerated(STRING) explicitly (the reference implementation this module
 * is based on left this off despite its schema column being textual, which
 * relied on enum ordinal position never changing).
 */
@Entity
@Table(name = "users")
public class UserJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(nullable = false)
    private String email;

    @Column(name = "avatar_seed", nullable = false)
    private String avatarSeed;

    @Column(name = "is_enabled", nullable = false)
    private boolean enabled;

    @Embedded
    private OneTimePasswordEmbeddable otp;

    @Column(name = "current_streak", nullable = false)
    private int currentStreak;

    @Column(name = "last_streak_date")
    private LocalDate lastStreakDate;

    @Column(name = "is_bot", nullable = false)
    private boolean bot;

    protected UserJpaEntity() {
    }

    public UserJpaEntity(Long id, String username, String password, UserRole role, String email, String avatarSeed,
            boolean enabled, OneTimePasswordEmbeddable otp, int currentStreak, LocalDate lastStreakDate) {
        this(id, username, password, role, email, avatarSeed, enabled, otp, currentStreak, lastStreakDate, false);
    }

    public UserJpaEntity(Long id, String username, String password, UserRole role, String email, String avatarSeed,
            boolean enabled, OneTimePasswordEmbeddable otp, int currentStreak, LocalDate lastStreakDate,
            boolean bot) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
        this.email = email;
        this.avatarSeed = avatarSeed;
        this.enabled = enabled;
        this.otp = otp;
        this.currentStreak = currentStreak;
        this.lastStreakDate = lastStreakDate;
        this.bot = bot;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public UserRole getRole() {
        return role;
    }

    public String getEmail() {
        return email;
    }

    public String getAvatarSeed() {
        return avatarSeed;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public OneTimePasswordEmbeddable getOtp() {
        return otp;
    }

    public int getCurrentStreak() {
        return currentStreak;
    }

    public LocalDate getLastStreakDate() {
        return lastStreakDate;
    }

    public boolean isBot() {
        return bot;
    }
}
