package com.kawser.cleanspringbootproject.game.domain.model;

import com.kawser.cleanspringbootproject.game.domain.exception.NotYourTurnException;
import com.kawser.cleanspringbootproject.game.domain.exception.PlayerNotFoundException;
import com.kawser.cleanspringbootproject.game.domain.exception.SelfVoteNotAllowedException;
import com.kawser.cleanspringbootproject.game.domain.exception.WrongPhaseException;
import com.kawser.cleanspringbootproject.game.domain.service.ScoringPolicy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A single game's word chain: every phase's dealt words, computed and known
 * in full before the first turn is played, the turn order (including which
 * players are secretly infiltrators — possibly none, for a room too small
 * to field one), every attempted word across every phase (accepted,
 * rejected, or skipped on timeout), and the votes cast against suspected
 * infiltrators. Owns the WORD_CHAIN -> VOTING -> REVEAL phase transitions
 * (or the WORD_CHAIN -> REVEAL shortcut when there are no infiltrators to
 * vote on, or when an infiltrator target word ends the game outright) and
 * the invariants tied to each phase.
 *
 * <p>A game can be made of several chained phases (see {@link
 * #advancePhase()}): once one phase's chain reaches its target, the next
 * phase begins — its start word is fixed at construction time to the
 * previous phase's own group target word, not whatever word actually gets
 * played, so the whole chain is known up front and can be shown to players
 * from the very start of the game. VOTING/REVEAL happen at most once, only
 * after the last phase. The {@code attempts} log is never cleared between
 * phases — it stays one continuous transcript for the whole game — so
 * {@link #latestChainWord()} naturally keeps working unchanged, and each
 * {@link ChainAttempt} records which phase it belongs to.
 */
public class Round {

    private final List<WordSet> phaseWordSets;
    private final int totalPhases;
    private final List<String> turnOrder;
    private final Set<String> infiltratorPlayerIds;
    private final List<ChainAttempt> attempts = new ArrayList<>();
    private final List<Vote> votes = new ArrayList<>();
    private int currentTurnIndex;
    private int currentPhaseIndex;
    private int phaseStartAttemptCount;
    private RoundPhase phase;
    private Instant phaseDeadline;
    private ChainResult result;

    /**
     * @param phaseWordSets every phase's dealt WordSet, in order, already
     *                      fully computed (see ChainWordBank.fullChain) —
     *                      its size must equal totalPhases.
     */
    public Round(List<WordSet> phaseWordSets, List<String> turnOrder, Set<String> infiltratorPlayerIds, int totalPhases) {
        if (turnOrder == null || turnOrder.isEmpty()) {
            throw new IllegalArgumentException("turnOrder must have at least 1 player");
        }
        if (!turnOrder.containsAll(infiltratorPlayerIds)) {
            throw new IllegalArgumentException("infiltratorPlayerIds must all be members of turnOrder");
        }
        if (totalPhases < 1) {
            throw new IllegalArgumentException("totalPhases must be at least 1");
        }
        if (phaseWordSets == null || phaseWordSets.size() != totalPhases) {
            throw new IllegalArgumentException("phaseWordSets must contain exactly totalPhases entries");
        }
        this.phaseWordSets = new ArrayList<>(phaseWordSets);
        this.totalPhases = totalPhases;
        this.turnOrder = List.copyOf(turnOrder);
        this.infiltratorPlayerIds = Set.copyOf(infiltratorPlayerIds);
        this.currentTurnIndex = 0;
        this.currentPhaseIndex = 0;
        this.phaseStartAttemptCount = 0;
        this.phase = RoundPhase.WORD_CHAIN;
    }

    public String currentTurnPlayerId() {
        return turnOrder.get(currentTurnIndex);
    }

    public void requireCurrentTurn(String playerId) {
        requirePhase(RoundPhase.WORD_CHAIN);
        if (!currentTurnPlayerId().equals(playerId)) {
            throw new NotYourTurnException(playerId);
        }
    }

    /**
     * The word each player is privately steering toward: every infiltrator
     * shares the same target, which differs from everyone else's, but
     * nothing here (or anywhere else in Round) ever tells a player which
     * case they are in.
     */
    public String targetWordFor(String playerId) {
        WordSet current = wordSet();
        return infiltratorPlayerIds.contains(playerId) ? current.infiltratorTargetWord() : current.groupTargetWord();
    }

    /**
     * The word the next submission must relate to: the most recently
     * ACCEPTED attempt, or the dealt start word if nothing has been
     * accepted yet.
     */
    public String latestChainWord() {
        for (int i = attempts.size() - 1; i >= 0; i--) {
            ChainAttempt attempt = attempts.get(i);
            if (attempt.outcome() == AttemptOutcome.ACCEPTED) {
                return attempt.text();
            }
        }
        return wordSet().startWord();
    }

    /**
     * Turns played in the CURRENT phase — resets to 0 each time {@link
     * #advancePhase()} runs. The full-game attempt count remains
     * available via {@code attempts().size()}.
     */
    public int turnsPlayed() {
        return attempts.size() - phaseStartAttemptCount;
    }

    /**
     * Whether an (already-accepted) word completes the current phase: the
     * crew's shared target for this phase, reached by anyone, including an
     * infiltrator by sheer coincidence. This only ever advances the phase
     * (or, on the last phase, moves on to voting/finish) — it never ends
     * the game as a loss. See {@link #reachesInfiltratorTarget} for the
     * separate, game-ending condition.
     */
    public boolean reachesGroupTarget(String text) {
        return text.trim().equalsIgnoreCase(wordSet().groupTargetWord().trim());
    }

    /**
     * Whether an (already-accepted) word gives away the infiltrators' own
     * shared secret target for the current phase — but only when written
     * by someone who isn't one of them. An infiltrator completing their own
     * secret target themselves does not count; nothing here (or in the
     * resulting attempt) lets them find that out, since doing so would
     * give their role away. Written by a non-infiltrator, this
     * immediately ends the whole game as a loss for the crew (see
     * Room/GameApplicationService), regardless of how many phases remain.
     */
    public boolean reachesInfiltratorTarget(String authorPlayerId, String text) {
        return text.trim().equalsIgnoreCase(wordSet().infiltratorTargetWord().trim())
                && !infiltratorPlayerIds.contains(authorPlayerId);
    }

    /**
     * Records the outcome of the current turn player's submission — always
     * added to the visible history, whether ACCEPTED or REJECTED, so
     * everyone can see why a rejected attempt didn't move the chain — and
     * advances the turn regardless of the outcome.
     */
    public ChainAttempt submitWord(String authorPlayerId, String text, WordJudgement judgement) {
        requireCurrentTurn(authorPlayerId);
        int turnNumber = attempts.size() + 1;
        ChainAttempt attempt = judgement.accepted()
                ? ChainAttempt.accepted(
                        authorPlayerId, turnNumber, text,
                        judgement.relatednessToPrevious(), judgement.justification(),
                        judgement.relatednessToTarget(), judgement.reachedTarget(),
                        currentPhaseIndex)
                : ChainAttempt.rejected(
                        authorPlayerId, turnNumber, text,
                        judgement.relatednessToPrevious(), judgement.justification(), currentPhaseIndex);
        attempts.add(attempt);
        advanceTurn();
        return attempt;
    }

    /**
     * Timeout path: the current turn player didn't submit anything in
     * time. Their turn is skipped without penalty and the last accepted
     * word remains the reference for whoever plays next.
     */
    public void skipCurrentTurn(String expectedPlayerId) {
        requireCurrentTurn(expectedPlayerId);
        attempts.add(ChainAttempt.skipped(expectedPlayerId, attempts.size() + 1, currentPhaseIndex));
        advanceTurn();
    }

    private void advanceTurn() {
        currentTurnIndex = (currentTurnIndex + 1) % turnOrder.size();
    }

    public void startVoting(Instant deadline) {
        requirePhase(RoundPhase.WORD_CHAIN);
        this.phase = RoundPhase.VOTING;
        this.phaseDeadline = deadline;
    }

    /**
     * A player may accuse as many times as they like while VOTING is
     * active; each call replaces that player's previous accusation rather
     * than rejecting it, mirroring the free/changeable vote already used
     * for the AI-spotting vote in the previous game mode.
     */
    public void submitVote(Vote vote) {
        requirePhase(RoundPhase.VOTING);
        if (!turnOrder.contains(vote.suspectPlayerId())) {
            throw new PlayerNotFoundException(vote.suspectPlayerId());
        }
        if (vote.voterPlayerId().equals(vote.suspectPlayerId())) {
            throw new SelfVoteNotAllowedException(vote.voterPlayerId());
        }
        votes.removeIf(existing -> existing.voterPlayerId().equals(vote.voterPlayerId()));
        votes.add(vote);
    }

    public ChainResult reveal(ScoringPolicy scoringPolicy, Collection<Player> players) {
        requirePhase(RoundPhase.VOTING);
        this.phase = RoundPhase.REVEAL;
        this.phaseDeadline = null;
        this.result = scoringPolicy.scoreAccusation(this, players);
        return this.result;
    }

    /**
     * The no-infiltrator path: a room too small to field one (fewer than 3
     * players) plays a purely cooperative chain, so there is no one to
     * vote on — this jumps straight from WORD_CHAIN to REVEAL instead of
     * going through VOTING at all.
     */
    public ChainResult finishCooperatively() {
        requirePhase(RoundPhase.WORD_CHAIN);
        this.phase = RoundPhase.REVEAL;
        this.phaseDeadline = null;
        this.result = new ChainResult(Set.of(), null, null, true, Map.of(), false);
        return this.result;
    }

    /**
     * The instant-loss path: someone outside the infiltrators wrote their
     * secret target word (see {@link #reachesInfiltratorTarget}), which
     * ends the whole game right there as a loss for everyone but the
     * infiltrators — no matter which phase this happened on, and without
     * going through VOTING, since there is nothing left to accuse anyone
     * of.
     */
    public ChainResult loseToInfiltrator() {
        requirePhase(RoundPhase.WORD_CHAIN);
        this.phase = RoundPhase.REVEAL;
        this.phaseDeadline = null;
        this.result =
                new ChainResult(infiltratorPlayerIds, wordSet().infiltratorTargetWord(), null, false, Map.of(), true);
        return this.result;
    }

    private void requirePhase(RoundPhase expected) {
        if (phase != expected) {
            throw new WrongPhaseException(expected, phase);
        }
    }

    /** The active phase's dealt words. */
    public WordSet wordSet() {
        return phaseWordSets.get(currentPhaseIndex);
    }

    /** 0-based index of the phase currently in progress. */
    public int phaseIndex() {
        return currentPhaseIndex;
    }

    public int totalPhases() {
        return totalPhases;
    }

    public boolean hasMorePhases() {
        return currentPhaseIndex + 1 < totalPhases;
    }

    /** Every phase's dealt WordSet so far, in order (index 0 = phase 1). */
    public List<WordSet> phaseWordSets() {
        return List.copyOf(phaseWordSets);
    }

    /**
     * Every start/group-target/infiltrator-target word across every phase
     * of the whole precomputed chain (not just the ones played so far).
     */
    public Set<String> usedWords() {
        return phaseWordSets.stream()
                .flatMap(ws -> Stream.of(ws.startWord(), ws.groupTargetWord(), ws.infiltratorTargetWord()))
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Moves WORD_CHAIN into its next phase (already dealt at construction
     * time — see the class Javadoc): advances the phase index and resets
     * the per-phase turn counter, but leaves currentTurnIndex (rotation
     * continues uninterrupted), attempts (never cleared), and phase (still
     * WORD_CHAIN) untouched.
     */
    public void advancePhase() {
        requirePhase(RoundPhase.WORD_CHAIN);
        if (!hasMorePhases()) {
            throw new IllegalStateException("no more phases remain");
        }
        currentPhaseIndex++;
        phaseStartAttemptCount = attempts.size();
    }

    public List<String> turnOrder() {
        return List.copyOf(turnOrder);
    }

    public Set<String> infiltratorPlayerIds() {
        return infiltratorPlayerIds;
    }

    public RoundPhase phase() {
        return phase;
    }

    public Instant phaseDeadline() {
        return phaseDeadline;
    }

    public void setPhaseDeadline(Instant phaseDeadline) {
        this.phaseDeadline = phaseDeadline;
    }

    public List<ChainAttempt> attempts() {
        return List.copyOf(attempts);
    }

    public List<Vote> votes() {
        return List.copyOf(votes);
    }

    public ChainResult result() {
        return result;
    }
}
