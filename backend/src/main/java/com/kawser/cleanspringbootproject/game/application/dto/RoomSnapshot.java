package com.kawser.cleanspringbootproject.game.application.dto;

import com.kawser.cleanspringbootproject.game.domain.model.AttemptOutcome;
import com.kawser.cleanspringbootproject.game.domain.model.ChainAttempt;
import com.kawser.cleanspringbootproject.game.domain.model.ChainResult;
import com.kawser.cleanspringbootproject.game.domain.model.GameLanguage;
import com.kawser.cleanspringbootproject.game.domain.model.Player;
import com.kawser.cleanspringbootproject.game.domain.model.Room;
import com.kawser.cleanspringbootproject.game.domain.model.RoomSettings;
import com.kawser.cleanspringbootproject.game.domain.model.RoomStatus;
import com.kawser.cleanspringbootproject.game.domain.model.Round;
import com.kawser.cleanspringbootproject.game.domain.model.RoundPhase;
import com.kawser.cleanspringbootproject.game.domain.model.Vote;
import com.kawser.cleanspringbootproject.game.domain.model.WordSet;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Read-only projection of a Room, personalized per viewer and pushed to
 * that viewer's own private queue on every change (no shared broadcast, no
 * incremental events — see RoomNotifier/StompRoomNotifier). This builder is
 * the single source of truth for every secrecy rule, so no adapter has to
 * re-derive them:
 *   - a player's reconnectToken never leaves Player.
 *   - the infiltrator's identity and target word never leave Round except
 *     inside RevealView, once the chain has reached REVEAL.
 *   - the numeric AI relatedness-to-target score behind an attempt is only
 *     shown to that attempt's own author before REVEAL (the fact that an
 *     attempt reached its author's target is still shown to everyone right
 *     away, since that is what publicly ends the chain and starts voting).
 */
public record RoomSnapshot(
        String roomCode,
        RoomStatus status,
        String hostPlayerId,
        List<PlayerView> players,
        SettingsView settings,
        String viewerPlayerId,
        ChainView chain) {

    public static RoomSnapshot from(Room room, String viewerPlayerId) {
        ChainView chainView = room.hasRound() ? ChainView.from(room.round(), viewerPlayerId) : null;
        return new RoomSnapshot(
                room.code(),
                room.status(),
                room.hostPlayerId(),
                room.players().stream().map(PlayerView::from).toList(),
                SettingsView.from(room.settings()),
                viewerPlayerId,
                chainView);
    }

    public record SettingsView(
            int wordTimeSeconds, int voteTimeSeconds, GameLanguage language, int infiltratorCount,
            int phaseCount, boolean daily) {
        static SettingsView from(RoomSettings settings) {
            return new SettingsView(
                    settings.wordTimeSeconds(), settings.voteTimeSeconds(), settings.language(),
                    settings.infiltratorCount(), settings.phaseCount(), settings.daily());
        }
    }

    public record PlayerView(String id, String name, int score, boolean connected, boolean host) {
        static PlayerView from(Player player) {
            return new PlayerView(player.id(), player.name(), player.score(), player.isConnected(), player.isHost());
        }
    }

    public record ChainView(
            RoundPhase phase,
            String startWord,
            String yourTargetWord,
            String currentWord,
            String currentTurnPlayerId,
            Instant phaseDeadline,
            int infiltratorCount,
            int currentPhaseNumber,
            int totalPhases,
            List<String> phaseStartWords,
            List<String> yourPhaseTargetWords,
            List<AttemptView> attempts,
            List<VoteView> votes,
            RevealView reveal) {

        static ChainView from(Round round, String viewerPlayerId) {
            boolean revealed = round.phase() == RoundPhase.REVEAL;
            List<AttemptView> attemptViews = round.attempts().stream()
                    .map(attempt -> AttemptView.from(attempt, viewerPlayerId, revealed))
                    .toList();
            List<VoteView> voteViews = round.votes().stream().map(VoteView::from).toList();
            RevealView revealView = revealed ? RevealView.from(round.result(), round.attempts()) : null;
            List<WordSet> phaseWordSets = round.phaseWordSets();
            // The whole chain is dealt up front (see ChainWordBank.fullChain),
            // so every phase's start word — and this viewer's own target for
            // every phase — is already known and safe to show from the very
            // start of the game: it's either the shared crew target (public
            // once revealed to the crew) or this viewer's own private
            // infiltrator target, never someone else's secret.
            boolean viewerIsInfiltrator = round.infiltratorPlayerIds().contains(viewerPlayerId);
            List<String> phaseStartWords = phaseWordSets.stream().map(WordSet::startWord).toList();
            List<String> yourPhaseTargetWords = phaseWordSets.stream()
                    .map(ws -> viewerIsInfiltrator ? ws.infiltratorTargetWord() : ws.groupTargetWord())
                    .toList();
            return new ChainView(
                    round.phase(),
                    round.wordSet().startWord(),
                    round.targetWordFor(viewerPlayerId),
                    round.latestChainWord(),
                    round.phase() == RoundPhase.WORD_CHAIN ? round.currentTurnPlayerId() : null,
                    round.phaseDeadline(),
                    round.infiltratorPlayerIds().size(),
                    round.phaseIndex() + 1,
                    round.totalPhases(),
                    phaseStartWords,
                    yourPhaseTargetWords,
                    attemptViews,
                    voteViews,
                    revealView);
        }
    }

    public record AttemptView(
            String id,
            String authorPlayerId,
            int turnNumber,
            String text,
            AttemptOutcome outcome,
            int relatednessToPrevious,
            String justification,
            Integer relatednessToTarget,
            boolean reachedTarget,
            int phaseIndex) {

        static AttemptView from(ChainAttempt attempt, String viewerPlayerId, boolean revealed) {
            boolean showTargetPercentage = revealed || viewerPlayerId.equals(attempt.authorPlayerId());
            return new AttemptView(
                    attempt.id(),
                    attempt.authorPlayerId(),
                    attempt.turnNumber(),
                    attempt.text(),
                    attempt.outcome(),
                    attempt.relatednessToPrevious(),
                    attempt.justification(),
                    showTargetPercentage ? attempt.relatednessToTarget() : null,
                    attempt.reachedTarget(),
                    attempt.phaseIndex());
        }
    }

    public record VoteView(String voterPlayerId, String suspectPlayerId) {
        static VoteView from(Vote vote) {
            return new VoteView(vote.voterPlayerId(), vote.suspectPlayerId());
        }
    }

    public record RevealView(
            List<String> infiltratorPlayerIds,
            String infiltratorTargetWord,
            String accusedPlayerId,
            boolean crewWon,
            Map<String, Integer> scoreDeltaByPlayerId,
            boolean endedByInfiltratorWord,
            List<String> acceptedWordChain,
            List<Integer> acceptedWordCountByPhase) {

        static RevealView from(ChainResult result, List<ChainAttempt> attempts) {
            // The full path actually walked, kept for the whole game rather
            // than reset per phase (see Round's attempts log) — only
            // ACCEPTED words ever moved the chain forward, so only those are
            // worth showing back once the game is over. acceptedWordCountByPhase
            // answers "how many words did it take to complete each phase":
            // index i is phase i+1's own accepted-word count.
            List<String> acceptedWordChain = attempts.stream()
                    .filter(attempt -> attempt.outcome() == AttemptOutcome.ACCEPTED)
                    .map(ChainAttempt::text)
                    .toList();
            Map<Integer, Long> countsByPhaseIndex = attempts.stream()
                    .filter(attempt -> attempt.outcome() == AttemptOutcome.ACCEPTED)
                    .collect(Collectors.groupingBy(ChainAttempt::phaseIndex, Collectors.counting()));
            int highestPhaseIndex = attempts.stream().mapToInt(ChainAttempt::phaseIndex).max().orElse(0);
            List<Integer> acceptedWordCountByPhase = IntStream.rangeClosed(0, highestPhaseIndex)
                    .mapToObj(phaseIndex -> countsByPhaseIndex.getOrDefault(phaseIndex, 0L).intValue())
                    .toList();
            return new RevealView(
                    List.copyOf(result.infiltratorPlayerIds()),
                    result.infiltratorTargetWord(),
                    result.accusedPlayerId(),
                    result.crewWon(),
                    result.scoreDeltaByPlayerId(),
                    result.endedByInfiltratorWord(),
                    acceptedWordChain,
                    acceptedWordCountByPhase);
        }
    }
}
