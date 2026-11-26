package com.kawser.cleanspringbootproject.feedback.application.service;

import com.kawser.cleanspringbootproject.feedback.application.dto.SubmitFeedbackCommand;
import com.kawser.cleanspringbootproject.feedback.application.port.out.FeedbackRepository;
import com.kawser.cleanspringbootproject.feedback.domain.exception.EmptyFeedbackException;
import com.kawser.cleanspringbootproject.feedback.domain.exception.FeedbackAlreadySubmittedException;
import com.kawser.cleanspringbootproject.feedback.domain.model.NotificationRecord;
import com.kawser.cleanspringbootproject.feedback.domain.model.NotificationStatus;
import com.kawser.cleanspringbootproject.feedback.domain.model.NotificationTopic;
import com.kawser.cleanspringbootproject.user.application.port.out.UserRepository;
import com.kawser.cleanspringbootproject.user.domain.model.User;
import com.kawser.cleanspringbootproject.user.domain.model.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedbackApplicationServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 26);
    private static final NotificationTopic TOPIC = NotificationTopic.CREATOR_LETTER;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FeedbackRepository feedbackRepository;

    private FeedbackApplicationService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-26T12:00:00Z"), ZoneOffset.UTC);
        service = new FeedbackApplicationService(userRepository, feedbackRepository, clock);
    }

    private User userWithGames(int dailyGamesPlayed) {
        User user = new User(7L, "player", "hash", "player@example.com", true, UserRole.USER,
                "avatar", null, 0, null, dailyGamesPlayed);
        when(userRepository.findByUsername("player")).thenReturn(Optional.of(user));
        return user;
    }

    @Test
    void showsLetterToFrequentPlayersWhoNeverInteractedWithIt() {
        userWithGames(6);
        when(feedbackRepository.find(7L, TOPIC)).thenReturn(Optional.empty());

        assertThat(service.shouldShowCreatorLetter("player")).isTrue();
    }

    @Test
    void hidesLetterForTheRestOfTheDayAfterDismissal() {
        userWithGames(6);
        when(feedbackRepository.find(7L, TOPIC)).thenReturn(Optional.of(
                new NotificationRecord(7L, TOPIC, NotificationStatus.DISMISSED, TODAY)));

        assertThat(service.shouldShowCreatorLetter("player")).isFalse();
    }

    @Test
    void dismissStoresTodaysUtcDate() {
        userWithGames(6);
        when(feedbackRepository.find(7L, TOPIC)).thenReturn(Optional.empty());

        service.dismissCreatorLetter("player");

        verify(feedbackRepository).saveDismissal(7L, "player", TOPIC, TODAY);
    }

    @Test
    void dismissNeverOverwritesAnAnswer() {
        userWithGames(6);
        when(feedbackRepository.find(7L, TOPIC)).thenReturn(Optional.of(
                new NotificationRecord(7L, TOPIC, NotificationStatus.ANSWERED, null)));

        service.dismissCreatorLetter("player");

        verify(feedbackRepository, never()).saveDismissal(any(), anyString(), any(), any());
    }

    @Test
    void submitStoresTrimmedAnswerAndTreatsBlankFieldsAsMissing() {
        userWithGames(6);
        when(feedbackRepository.find(7L, TOPIC)).thenReturn(Optional.empty());

        service.submitCreatorLetterFeedback(new SubmitFeedbackCommand("player", "  more modes  ", "   "));

        verify(feedbackRepository).saveAnswer(eq(7L), eq("player"), eq(TOPIC), eq("more modes"), eq(null));
    }

    @Test
    void submitRejectsAnAllBlankForm() {
        assertThatThrownBy(() -> service.submitCreatorLetterFeedback(new SubmitFeedbackCommand("player", " ", null)))
                .isInstanceOf(EmptyFeedbackException.class);
    }

    @Test
    void submitRejectsASecondAnswer() {
        userWithGames(6);
        when(feedbackRepository.find(7L, TOPIC)).thenReturn(Optional.of(
                new NotificationRecord(7L, TOPIC, NotificationStatus.ANSWERED, null)));

        assertThatThrownBy(() -> service.submitCreatorLetterFeedback(
                new SubmitFeedbackCommand("player", "again", null)))
                .isInstanceOf(FeedbackAlreadySubmittedException.class);
    }
}
