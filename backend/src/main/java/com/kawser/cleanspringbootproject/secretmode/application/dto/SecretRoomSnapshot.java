package com.kawser.cleanspringbootproject.secretmode.application.dto;

import com.kawser.cleanspringbootproject.game.domain.model.GameLanguage;
import com.kawser.cleanspringbootproject.secretmode.domain.model.JudgementSource;
import com.kawser.cleanspringbootproject.secretmode.domain.model.SecretRoom;
import com.kawser.cleanspringbootproject.secretmode.domain.model.SecretRoomStatus;
import com.kawser.cleanspringbootproject.secretmode.domain.model.SolutionAttempt;
import com.kawser.cleanspringbootproject.secretmode.domain.model.SolutionMatchLevel;
import com.kawser.cleanspringbootproject.secretmode.domain.model.WordAttempt;
import com.kawser.cleanspringbootproject.secretmode.domain.model.WordAttemptOutcome;

import java.time.Instant;
import java.util.List;

/**
 * Read-only projection of a SecretRoom, pushed to the client on every
 * change (see SecretRoomNotifier). Unlike game.application.dto.RoomSnapshot
 * there is no viewerPlayerId/per-player secrecy split to worry about — a
 * solo room's one player sees the whole of their own state — so this is
 * considerably simpler: the only thing ever withheld before the room ends
 * is the concept's hiddenQuality itself (see RevealView).
 */
public record SecretRoomSnapshot(
        String roomCode,
        SecretRoomStatus status,
        GameLanguage language,
        String conceptId,
        List<WordAttemptView> attempts,
        int totalFailures,
        int failuresPerHint,
        int hintsAvailable,
        int hintsUsed,
        List<SolutionAttemptView> solutionAttempts,
        RevealView reveal,
        Instant createdAt) {

    public static SecretRoomSnapshot from(SecretRoom room) {
        boolean revealed = room.status() == SecretRoomStatus.SOLVED || room.status() == SecretRoomStatus.GAVE_UP;
        return new SecretRoomSnapshot(
                room.code(),
                room.status(),
                room.concept().language(),
                room.concept().id(),
                room.attempts().stream().map(WordAttemptView::from).toList(),
                room.totalFailures(),
                SecretRoom.FAILURES_PER_HINT,
                room.hintsAvailable(),
                room.hintsUsed(),
                room.solutionAttempts().stream().map(SolutionAttemptView::from).toList(),
                revealed ? RevealView.from(room) : null,
                room.createdAt());
    }

    public record WordAttemptView(String id, String text, WordAttemptOutcome outcome, JudgementSource source, Instant submittedAt) {
        static WordAttemptView from(WordAttempt attempt) {
            return new WordAttemptView(attempt.id(), attempt.text(), attempt.outcome(), attempt.source(), attempt.submittedAt());
        }
    }

    public record SolutionAttemptView(String id, String formulationText, SolutionMatchLevel matchLevel, Instant submittedAt) {
        static SolutionAttemptView from(SolutionAttempt attempt) {
            return new SolutionAttemptView(attempt.id(), attempt.formulationText(), attempt.matchLevel(), attempt.submittedAt());
        }
    }

    public record RevealView(
            String hiddenQuality,
            Integer finalScore,
            int totalAttempts,
            int acceptedAttempts,
            int rejectedAttempts,
            int hintsUsed) {
        static RevealView from(SecretRoom room) {
            List<WordAttempt> attempts = room.attempts();
            long accepted = attempts.stream().filter(a -> a.outcome() == WordAttemptOutcome.ACCEPTED).count();
            long rejected = attempts.stream().filter(a -> a.outcome() == WordAttemptOutcome.REJECTED).count();
            return new RevealView(
                    room.concept().hiddenQuality(), room.finalScore(), attempts.size(),
                    (int) accepted, (int) rejected, room.hintsUsed());
        }
    }
}
