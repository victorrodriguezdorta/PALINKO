package com.kawser.cleanspringbootproject.feedback.application.service;

import com.kawser.cleanspringbootproject.feedback.application.dto.SubmitFeedbackCommand;
import com.kawser.cleanspringbootproject.feedback.application.port.in.DismissCreatorLetterUseCase;
import com.kawser.cleanspringbootproject.feedback.application.port.in.GetCreatorLetterStatusUseCase;
import com.kawser.cleanspringbootproject.feedback.application.port.in.SubmitCreatorLetterFeedbackUseCase;
import com.kawser.cleanspringbootproject.feedback.application.port.out.FeedbackRepository;
import com.kawser.cleanspringbootproject.feedback.domain.exception.EmptyFeedbackException;
import com.kawser.cleanspringbootproject.feedback.domain.exception.FeedbackAlreadySubmittedException;
import com.kawser.cleanspringbootproject.feedback.domain.model.CreatorLetterPolicy;
import com.kawser.cleanspringbootproject.feedback.domain.model.NotificationRecord;
import com.kawser.cleanspringbootproject.feedback.domain.model.NotificationTopic;
import com.kawser.cleanspringbootproject.user.application.port.out.UserRepository;
import com.kawser.cleanspringbootproject.user.domain.exception.UserNotFoundException;
import com.kawser.cleanspringbootproject.user.domain.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

@Service
public class FeedbackApplicationService implements
        GetCreatorLetterStatusUseCase, SubmitCreatorLetterFeedbackUseCase, DismissCreatorLetterUseCase {

    private static final NotificationTopic TOPIC = NotificationTopic.CREATOR_LETTER;

    private final UserRepository userRepository;
    private final FeedbackRepository feedbackRepository;
    private final Clock clock;

    @Autowired
    public FeedbackApplicationService(UserRepository userRepository, FeedbackRepository feedbackRepository) {
        this(userRepository, feedbackRepository, Clock.systemUTC());
    }

    FeedbackApplicationService(UserRepository userRepository, FeedbackRepository feedbackRepository, Clock clock) {
        this.userRepository = userRepository;
        this.feedbackRepository = feedbackRepository;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean shouldShowCreatorLetter(String username) {
        User user = findUser(username);
        return CreatorLetterPolicy.shouldShow(
                user.dailyGamesPlayed(), feedbackRepository.find(user.id(), TOPIC), today());
    }

    @Override
    @Transactional
    public void submitCreatorLetterFeedback(SubmitFeedbackCommand command) {
        String suggestion = normalize(command.suggestion());
        String issues = normalize(command.issues());
        if (suggestion == null && issues == null) {
            throw new EmptyFeedbackException();
        }
        User user = findUser(command.username());
        if (alreadyAnswered(user)) {
            throw new FeedbackAlreadySubmittedException();
        }
        feedbackRepository.saveAnswer(user.id(), user.username(), TOPIC, suggestion, issues);
    }

    @Override
    @Transactional
    public void dismissCreatorLetter(String username) {
        User user = findUser(username);
        // An answered letter stays answered: a stale tab closing it must not undo that.
        if (alreadyAnswered(user)) {
            return;
        }
        feedbackRepository.saveDismissal(user.id(), user.username(), TOPIC, today());
    }

    private boolean alreadyAnswered(User user) {
        return feedbackRepository.find(user.id(), TOPIC).map(NotificationRecord::answered).orElse(false);
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username).orElseThrow(UserNotFoundException::new);
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
