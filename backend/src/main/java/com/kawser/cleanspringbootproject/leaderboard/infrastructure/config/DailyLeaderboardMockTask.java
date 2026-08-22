package com.kawser.cleanspringbootproject.leaderboard.infrastructure.config;

import com.kawser.cleanspringbootproject.leaderboard.infrastructure.adapter.out.persistence.DailyLeaderboardEntryJpaEntity;
import com.kawser.cleanspringbootproject.leaderboard.infrastructure.adapter.out.persistence.DailyLeaderboardEntryJpaRepository;
import com.kawser.cleanspringbootproject.user.application.port.out.PasswordHasher;
import com.kawser.cleanspringbootproject.user.domain.model.UserRole;
import com.kawser.cleanspringbootproject.user.infrastructure.adapter.out.persistence.UserJpaEntity;
import com.kawser.cleanspringbootproject.user.infrastructure.adapter.out.persistence.UserJpaRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * Keeps the daily leaderboard populated with a handful of always-beatable
 * "phantom" players so a brand-new day never opens on an empty board: real
 * results are wiped daily (see clearYesterday) rather than kept around, so
 * without this the board would be empty until the first real player of the
 * day finished. The bot accounts themselves are created once and reused
 * forever — only their score entry and streak are refreshed each day.
 */
@Component
public class DailyLeaderboardMockTask {

    private static final int BOT_COUNT = 10;
    private static final int MIN_WORD_COUNT = 15;
    private static final int MAX_WORD_COUNT = 20;
    private static final int MIN_ACCURACY_PERCENT = 70;
    private static final int MAX_ACCURACY_PERCENT = 100;
    private static final int MIN_STREAK_DAYS = 0;
    private static final int MAX_STREAK_DAYS = 6;
    private static final String[] BOT_NAMES = {
            "LexiFox", "WordWisp", "SlyOtter", "QuietRaven", "PixelHare",
            "NovaLynx", "EchoWren", "ZestyPuma", "MintCrow", "SilentIbis"
    };

    private final UserJpaRepository userJpaRepository;
    private final DailyLeaderboardEntryJpaRepository leaderboardEntryJpaRepository;
    private final PasswordHasher passwordHasher;
    private final SecureRandom random = new SecureRandom();

    public DailyLeaderboardMockTask(
            UserJpaRepository userJpaRepository,
            DailyLeaderboardEntryJpaRepository leaderboardEntryJpaRepository,
            PasswordHasher passwordHasher) {
        this.userJpaRepository = userJpaRepository;
        this.leaderboardEntryJpaRepository = leaderboardEntryJpaRepository;
        this.passwordHasher = passwordHasher;
    }

    @Scheduled(cron = "0 0 0 * * *", zone = "UTC")
    public void refreshDailyBoard() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        clearStaleEntries(today);
        seedBotResults(today);
    }

    /** Drops every leaderboard entry that isn't for today — nothing older is worth keeping around. */
    private void clearStaleEntries(LocalDate today) {
        leaderboardEntryJpaRepository.deleteByChallengeDateNot(today);
    }

    private void seedBotResults(LocalDate today) {
        List<UserJpaEntity> bots = ensureBotAccountsExist();
        for (UserJpaEntity bot : bots) {
            int wordCount = MIN_WORD_COUNT + random.nextInt(MAX_WORD_COUNT - MIN_WORD_COUNT + 1);
            int accuracyPercent = MIN_ACCURACY_PERCENT + random.nextInt(MAX_ACCURACY_PERCENT - MIN_ACCURACY_PERCENT + 1);
            Instant completedAt = todayInstantWithRandomOffset(today);
            // Re-seeding the same day (e.g. via the debug reseed endpoint) would
            // otherwise violate uq_daily_leaderboard_user_date on the second call.
            DailyLeaderboardEntryJpaEntity entry = leaderboardEntryJpaRepository
                    .findByUserIdAndChallengeDate(bot.getId(), today)
                    .orElseGet(() -> new DailyLeaderboardEntryJpaEntity(null, bot.getId(), today, wordCount, accuracyPercent, completedAt));
            entry.setWordCount(wordCount);
            entry.setAccuracyPercent(accuracyPercent);
            entry.setCompletedAt(completedAt);
            leaderboardEntryJpaRepository.save(entry);
            userJpaRepository.save(withRandomStreak(bot, today));
        }
    }

    /** Gives the bot a fresh random streak each refresh, purely cosmetic for the leaderboard's flame badge. */
    private UserJpaEntity withRandomStreak(UserJpaEntity bot, LocalDate today) {
        int streakDays = MIN_STREAK_DAYS + random.nextInt(MAX_STREAK_DAYS - MIN_STREAK_DAYS + 1);
        LocalDate lastStreakDate = streakDays > 0 ? today : null;
        return new UserJpaEntity(bot.getId(), bot.getUsername(), bot.getPassword(), bot.getRole(), bot.getEmail(),
                bot.getAvatarSeed(), bot.isEnabled(), bot.getOtp(), streakDays, lastStreakDate, bot.isBot());
    }

    private Instant todayInstantWithRandomOffset(LocalDate today) {
        Instant startOfDay = today.atStartOfDay(ZoneOffset.UTC).toInstant();
        return startOfDay.plusSeconds(random.nextInt(24 * 60 * 60));
    }

    private List<UserJpaEntity> ensureBotAccountsExist() {
        List<UserJpaEntity> existingBots = userJpaRepository.findByBotTrue();
        if (existingBots.size() >= BOT_COUNT) {
            return existingBots;
        }
        String sharedPasswordHash = passwordHasher.hash(UUID.randomUUID().toString());
        for (int i = existingBots.size(); i < BOT_COUNT; i++) {
            UserJpaEntity bot = new UserJpaEntity(
                    null, BOT_NAMES[i % BOT_NAMES.length] + (i / BOT_NAMES.length == 0 ? "" : i),
                    sharedPasswordHash, UserRole.USER, "bot-" + UUID.randomUUID() + "@guesstheai.local",
                    randomAvatarSeed(), true, null, 0, null, true);
            existingBots.add(userJpaRepository.save(bot));
        }
        return existingBots;
    }

    private String randomAvatarSeed() {
        String alphabet = "abcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder seed = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            seed.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return seed.toString();
    }
}
