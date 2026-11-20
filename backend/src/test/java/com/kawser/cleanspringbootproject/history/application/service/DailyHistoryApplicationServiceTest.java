package com.kawser.cleanspringbootproject.history.application.service;

import com.kawser.cleanspringbootproject.history.application.port.out.DailyHistoryRepository;
import com.kawser.cleanspringbootproject.user.application.port.out.UserRepository;
import com.kawser.cleanspringbootproject.user.domain.model.User;
import com.kawser.cleanspringbootproject.user.domain.model.UserRole;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class DailyHistoryApplicationServiceTest {

    @Test
    void returnsCompletedDatesForTheAuthenticatedUsersAccount() {
        User user = new User(42L, "player", "hash", "player@example.com", true, UserRole.USER,
                "avatar", null, 0, null);
        List<LocalDate> completedDates = List.of(LocalDate.of(2026, 11, 18), LocalDate.of(2026, 11, 19));
        UserRepository users = new UserRepository() {
            @Override
            public boolean existsByUsername(String username) {
                return false;
            }

            @Override
            public boolean existsByEmail(String email) {
                return false;
            }

            @Override
            public Optional<User> findByUsername(String username) {
                return username.equals(user.username()) ? Optional.of(user) : Optional.empty();
            }

            @Override
            public Optional<User> findByEmail(String email) {
                return Optional.empty();
            }

            @Override
            public User save(User savedUser) {
                throw new UnsupportedOperationException();
            }
        };
        DailyHistoryRepository history = new DailyHistoryRepository() {
            @Override
            public void upsertLatest(Long userId, String username, LocalDate challengeDate, String wordChain) {
                throw new UnsupportedOperationException();
            }

            @Override
            public List<LocalDate> findCompletedDates(Long userId) {
                assertThat(userId).isEqualTo(user.id());
                return completedDates;
            }
        };

        DailyHistoryApplicationService service = new DailyHistoryApplicationService(users, history);

        assertThat(service.getCompletedDates("player")).containsExactlyElementsOf(completedDates);
    }
}
