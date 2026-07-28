package com.kawser.cleanspringbootproject.game.application.dto;

import com.kawser.cleanspringbootproject.game.domain.model.Answer;
import com.kawser.cleanspringbootproject.game.domain.model.Player;
import com.kawser.cleanspringbootproject.game.domain.model.Room;
import com.kawser.cleanspringbootproject.game.domain.model.RoomSettings;
import com.kawser.cleanspringbootproject.game.domain.model.Round;
import com.kawser.cleanspringbootproject.game.domain.model.RoomStatus;
import com.kawser.cleanspringbootproject.game.domain.model.RoundPhase;
import com.kawser.cleanspringbootproject.game.domain.model.RoundResult;
import com.kawser.cleanspringbootproject.game.domain.model.Vote;

import java.time.Instant;
import java.util.List;

/**
 * Read-only projection of a Room, broadcast in full to every client on
 * every change (no incremental events). Never carries a Player's
 * reconnectToken, and never carries an answer's authorPlayerId unless the
 * round has reached REVEAL — this builder is the single source of truth
 * for both rules, so no adapter has to re-derive them.
 */
public record RoomSnapshot(
        String roomCode,
        RoomStatus status,
        String hostPlayerId,
        List<PlayerView> players,
        SettingsView settings,
        RoundView currentRound) {

    public static RoomSnapshot from(Room room) {
        RoundView roundView = hasCurrentRound(room) ? RoundView.from(room.currentRound(), null) : null;
        return new RoomSnapshot(
                room.code(),
                room.status(),
                room.hostPlayerId(),
                room.players().stream().map(PlayerView::from).toList(),
                SettingsView.from(room.settings()),
                roundView);
    }

    public static RoomSnapshot fromWithResult(Room room, RoundResult roundResult) {
        RoundView roundView = hasCurrentRound(room) ? RoundView.from(room.currentRound(), roundResult) : null;
        return new RoomSnapshot(
                room.code(),
                room.status(),
                room.hostPlayerId(),
                room.players().stream().map(PlayerView::from).toList(),
                SettingsView.from(room.settings()),
                roundView);
    }

    private static boolean hasCurrentRound(Room room) {
        return room.hasCurrentRound();
    }

    public record SettingsView(int totalRounds, int answerTimeSeconds, int voteTimeSeconds) {
        static SettingsView from(RoomSettings settings) {
            return new SettingsView(settings.totalRounds(), settings.answerTimeSeconds(), settings.voteTimeSeconds());
        }
    }

    public record PlayerView(String id, String name, int score, boolean connected, boolean host) {
        static PlayerView from(Player player) {
            return new PlayerView(player.id(), player.name(), player.score(), player.isConnected(), player.isHost());
        }
    }

    public record RoundView(
            int roundNumber,
            RoundPhase phase,
            String questionText,
            Instant phaseDeadline,
            List<AnswerView> answers,
            RoundResultView result) {

        static RoundView from(Round round, RoundResult roundResult) {
            boolean revealed = round.phase() == RoundPhase.REVEAL;
            List<Vote> votes = round.votes();
            List<AnswerView> answerViews = round.answers().stream()
                    .map(answer -> AnswerView.from(answer, revealed, votes))
                    .toList();
            RoundResultView resultView = (revealed && roundResult != null)
                    ? RoundResultView.from(roundResult)
                    : null;
            return new RoundView(
                    round.roundNumber(),
                    round.phase(),
                    round.question().text(),
                    round.phaseDeadline(),
                    answerViews,
                    resultView);
        }
    }

    public record AnswerView(String id, String text, String authorPlayerId, boolean isAi, List<String> voterPlayerIds) {
        static AnswerView from(Answer answer, boolean revealed, List<Vote> votes) {
            List<String> voters = votes.stream()
                    .filter(vote -> vote.answerId().equals(answer.id()))
                    .map(Vote::voterPlayerId)
                    .toList();
            return new AnswerView(
                    answer.id(),
                    answer.text(),
                    revealed ? answer.authorPlayerId() : null,
                    revealed && answer.isAi(),
                    voters);
        }
    }

    public record RoundResultView(String aiAnswerId, java.util.Map<String, Integer> scoreDeltaByPlayerId) {
        static RoundResultView from(RoundResult result) {
            return new RoundResultView(result.aiAnswerId(), result.scoreDeltaByPlayerId());
        }
    }
}
