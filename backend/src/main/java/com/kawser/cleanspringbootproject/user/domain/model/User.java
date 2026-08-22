package com.kawser.cleanspringbootproject.user.domain.model;

import java.time.LocalDate;

/**
 * Pure domain representation of an account — deliberately independent of
 * both persistence (no JPA annotations, see UserJpaEntity) and Spring
 * Security (does not implement UserDetails, see SpringUserDetailsAdapter).
 * Immutable: every state change (verifying, resetting the password,
 * issuing/consuming an OTP) returns a new instance via the with* methods
 * below rather than mutating in place.
 */
public final class User {

    private final Long id;
    private final String username;
    private final String passwordHash;
    private final String email;
    private final boolean enabled;
    private final UserRole role;
    private final String avatarSeed;
    private final OneTimePassword otp;
    private final int currentStreak;
    private final LocalDate lastStreakDate;

    public User(Long id, String username, String passwordHash, String email, boolean enabled, UserRole role,
            String avatarSeed, OneTimePassword otp, int currentStreak, LocalDate lastStreakDate) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
        this.enabled = enabled;
        this.role = role;
        this.avatarSeed = avatarSeed;
        this.otp = otp;
        this.currentStreak = currentStreak;
        this.lastStreakDate = lastStreakDate;
    }

    public static User newAccount(String username, String passwordHash, String email, String avatarSeed,
            boolean enabled, OneTimePassword otp) {
        return new User(null, username, passwordHash, email, enabled, UserRole.USER, avatarSeed, otp, 0, null);
    }

    public User withId(Long id) {
        return new User(id, username, passwordHash, email, enabled, role, avatarSeed, otp, currentStreak,
                lastStreakDate);
    }

    public User verified() {
        return new User(id, username, passwordHash, email, true, role, avatarSeed, null, currentStreak,
                lastStreakDate);
    }

    public User withOtp(OneTimePassword newOtp) {
        return new User(id, username, passwordHash, email, enabled, role, avatarSeed, newOtp, currentStreak,
                lastStreakDate);
    }

    public User withPasswordHash(String newPasswordHash) {
        return new User(id, username, newPasswordHash, email, enabled, role, avatarSeed, null, currentStreak,
                lastStreakDate);
    }

    public User withUsername(String newUsername) {
        return new User(id, newUsername, passwordHash, email, enabled, role, avatarSeed, otp, currentStreak,
                lastStreakDate);
    }

    public User withAvatarSeed(String newAvatarSeed) {
        return new User(id, username, passwordHash, email, enabled, role, newAvatarSeed, otp, currentStreak,
                lastStreakDate);
    }

    /**
     * Records that the daily challenge was completed on {@code today}
     * (always the UTC calendar day — see DailySeed.today, the same notion of
     * "day" the daily challenge itself is dealt from). Consecutive UTC days
     * extend the streak by one; a call on the same day the streak was last
     * extended is a no-op (finishing the same daily room twice must not
     * double-count); any gap of more than one day resets the streak to 1
     * rather than 0, since today's own completion always counts.
     */
    public User withDailyCompletion(LocalDate today) {
        if (today.equals(lastStreakDate)) {
            return this;
        }
        boolean consecutive = lastStreakDate != null && lastStreakDate.plusDays(1).equals(today);
        int newStreak = consecutive ? currentStreak + 1 : 1;
        return new User(id, username, passwordHash, email, enabled, role, avatarSeed, otp, newStreak, today);
    }

    /**
     * The streak as of {@code today}: still current if the account played
     * yesterday or today (UTC), otherwise it has silently lapsed to 0 even
     * though no write has recorded that yet — a read-only projection so the
     * lapse shows up immediately without a background job touching every
     * row at midnight.
     */
    public int currentStreakAsOf(LocalDate today) {
        if (lastStreakDate == null) {
            return 0;
        }
        boolean stillCurrent = lastStreakDate.equals(today) || lastStreakDate.plusDays(1).equals(today);
        return stillCurrent ? currentStreak : 0;
    }

    public Long id() {
        return id;
    }

    public String username() {
        return username;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public String email() {
        return email;
    }

    public boolean enabled() {
        return enabled;
    }

    public UserRole role() {
        return role;
    }

    public String avatarSeed() {
        return avatarSeed;
    }

    public OneTimePassword otp() {
        return otp;
    }

    public int currentStreak() {
        return currentStreak;
    }

    public LocalDate lastStreakDate() {
        return lastStreakDate;
    }
}
