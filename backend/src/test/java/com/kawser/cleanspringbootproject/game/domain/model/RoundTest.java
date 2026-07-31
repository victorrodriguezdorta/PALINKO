package com.kawser.cleanspringbootproject.game.domain.model;

import com.kawser.cleanspringbootproject.game.domain.exception.NotYourTurnException;
import com.kawser.cleanspringbootproject.game.domain.exception.PlayerNotFoundException;
import com.kawser.cleanspringbootproject.game.domain.exception.SelfVoteNotAllowedException;
import com.kawser.cleanspringbootproject.game.domain.exception.WrongPhaseException;
import com.kawser.cleanspringbootproject.game.domain.service.DefaultScoringPolicy;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoundTest {

    private static final List<String> TURN_ORDER = List.of("alice", "bob", "carol");
    private static final WordSet WORD_SET = new WordSet("Bolígrafo", "Cama", "Océano");
    private static final WordSet SECOND_WORD_SET = new WordSet("Cama", "Fresa", "Coche");

    private Round newRound() {
        return new Round(List.of(WORD_SET), TURN_ORDER, Set.of("carol"), 1);
    }

    private Round newRound(int totalPhases) {
        List<WordSet> wordSets = totalPhases == 1 ? List.of(WORD_SET) : List.of(WORD_SET, SECOND_WORD_SET);
        return new Round(wordSets, TURN_ORDER, Set.of("carol"), totalPhases);
    }

    @Test
    void startsOnFirstPlayersTurnInWordChainPhase() {
        Round round = newRound();

        assertThat(round.phase()).isEqualTo(RoundPhase.WORD_CHAIN);
        assertThat(round.currentTurnPlayerId()).isEqualTo("alice");
        assertThat(round.latestChainWord()).isEqualTo("Bolígrafo");
    }

    @Test
    void requireCurrentTurnRejectsAnyoneElse() {
        Round round = newRound();

        assertThatThrownBy(() -> round.requireCurrentTurn("bob"))
                .isInstanceOf(NotYourTurnException.class);
    }

    @Test
    void acceptedWordAdvancesTurnAndBecomesNewChainWord() {
        Round round = newRound();

        round.submitWord("alice", "Escribir", new WordJudgement(true, 80, null, 20, false));

        assertThat(round.latestChainWord()).isEqualTo("Escribir");
        assertThat(round.currentTurnPlayerId()).isEqualTo("bob");
        assertThat(round.turnsPlayed()).isEqualTo(1);
        assertThat(round.attempts()).hasSize(1);
        assertThat(round.attempts().get(0).outcome()).isEqualTo(AttemptOutcome.ACCEPTED);
    }

    @Test
    void rejectedWordAdvancesTurnButDoesNotChangeChainWord() {
        Round round = newRound();

        round.submitWord("alice", "Elefante", new WordJudgement(false, 5, null, null, false));

        assertThat(round.latestChainWord()).isEqualTo("Bolígrafo");
        assertThat(round.currentTurnPlayerId()).isEqualTo("bob");
        assertThat(round.attempts().get(0).outcome()).isEqualTo(AttemptOutcome.REJECTED);
    }

    @Test
    void skippedTurnAdvancesWithoutRecordingAWordOrChangingTheChain() {
        Round round = newRound();

        round.skipCurrentTurn("alice");

        assertThat(round.currentTurnPlayerId()).isEqualTo("bob");
        assertThat(round.latestChainWord()).isEqualTo("Bolígrafo");
        assertThat(round.attempts().get(0).outcome()).isEqualTo(AttemptOutcome.SKIPPED);
    }

    @Test
    void turnOrderWrapsAroundRoundRobin() {
        Round round = newRound();

        round.submitWord("alice", "Escribir", new WordJudgement(true, 80, null, 20, false));
        round.submitWord("bob", "Libro", new WordJudgement(true, 80, null, 20, false));
        round.submitWord("carol", "Biblioteca", new WordJudgement(true, 80, null, 20, false));

        assertThat(round.currentTurnPlayerId()).isEqualTo("alice");
        assertThat(round.turnsPlayed()).isEqualTo(3);
    }

    @Test
    void targetWordDiffersOnlyForTheInfiltrator() {
        Round round = newRound();

        assertThat(round.targetWordFor("carol")).isEqualTo("Océano");
        assertThat(round.targetWordFor("alice")).isEqualTo("Cama");
        assertThat(round.targetWordFor("bob")).isEqualTo("Cama");
    }

    @Test
    void reachesGroupTargetIsTrueRegardlessOfWhoWritesIt() {
        Round round = newRound();

        assertThat(round.reachesGroupTarget("Cama")).isTrue();
    }

    @Test
    void reachesInfiltratorTargetIsFalseWhenTheInfiltratorWritesTheirOwnSecretTarget() {
        Round round = newRound();

        assertThat(round.reachesInfiltratorTarget("carol", "Océano")).isFalse();
    }

    @Test
    void reachesInfiltratorTargetIsTrueWhenSomeoneElseWritesTheInfiltratorsSecretTarget() {
        Round round = newRound();

        assertThat(round.reachesInfiltratorTarget("alice", "Océano")).isTrue();
        assertThat(round.reachesInfiltratorTarget("bob", "Océano")).isTrue();
    }

    @Test
    void submitVoteReplacesTheVotersPreviousChoice() {
        Round round = newRound();
        round.startVoting(Instant.now().plusSeconds(30));

        round.submitVote(new Vote("alice", "bob"));
        round.submitVote(new Vote("alice", "carol"));

        assertThat(round.votes()).containsExactly(new Vote("alice", "carol"));
    }

    @Test
    void submitVoteRejectsAnUnknownSuspect() {
        Round round = newRound();
        round.startVoting(Instant.now().plusSeconds(30));

        assertThatThrownBy(() -> round.submitVote(new Vote("alice", "someone-else")))
                .isInstanceOf(PlayerNotFoundException.class);
    }

    @Test
    void submitVoteRejectsVotingForYourself() {
        Round round = newRound();
        round.startVoting(Instant.now().plusSeconds(30));

        assertThatThrownBy(() -> round.submitVote(new Vote("alice", "alice")))
                .isInstanceOf(SelfVoteNotAllowedException.class);
    }

    @Test
    void submitVoteRequiresVotingPhase() {
        Round round = newRound();

        assertThatThrownBy(() -> round.submitVote(new Vote("alice", "bob")))
                .isInstanceOf(WrongPhaseException.class);
    }

    @Test
    void revealRequiresVotingPhaseAndMovesToReveal() {
        Round round = newRound();

        assertThatThrownBy(() -> round.reveal(new DefaultScoringPolicy(), List.of()))
                .isInstanceOf(WrongPhaseException.class);

        round.startVoting(Instant.now().plusSeconds(30));
        round.reveal(new DefaultScoringPolicy(), List.of(
                Player.host("alice", "t1", "Alice", "seed-alice"),
                Player.guest("bob", "t2", "Bob", "seed-bob"),
                Player.guest("carol", "t3", "Carol", "seed-carol")));

        assertThat(round.phase()).isEqualTo(RoundPhase.REVEAL);
        assertThat(round.phaseDeadline()).isNull();
        assertThat(round.result()).isNotNull();
    }

    @Test
    void constructorRejectsPhaseCountLessThanOne() {
        assertThatThrownBy(() -> new Round(List.of(WORD_SET), TURN_ORDER, Set.of("carol"), 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructorRejectsPhaseWordSetsSizeMismatch() {
        assertThatThrownBy(() -> new Round(List.of(WORD_SET), TURN_ORDER, Set.of("carol"), 2))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void hasMorePhasesReflectsPhaseIndexVsTotalPhases() {
        Round oneRound = newRound(1);
        Round twoRounds = newRound(2);

        assertThat(oneRound.hasMorePhases()).isFalse();
        assertThat(twoRounds.hasMorePhases()).isTrue();
        assertThat(twoRounds.phaseIndex()).isEqualTo(0);
        assertThat(twoRounds.totalPhases()).isEqualTo(2);
    }

    @Test
    void usedWordsAggregatesEveryPhaseWordSetInTheWholePrecomputedChain() {
        Round round = newRound(2);

        assertThat(round.usedWords()).containsExactlyInAnyOrder(
                "Bolígrafo", "Cama", "Océano", "Fresa", "Coche");
    }

    @Test
    void advancePhaseMovesToNextWordSetKeepsWordChainPhaseAndResetsPerPhaseTurnCount() {
        Round round = newRound(2);
        round.submitWord("alice", "Escribir", new WordJudgement(true, 80, null, 20, false));
        round.submitWord("bob", "Cama", new WordJudgement(true, 90, null, 100, true));

        round.advancePhase();

        assertThat(round.phase()).isEqualTo(RoundPhase.WORD_CHAIN);
        assertThat(round.phaseIndex()).isEqualTo(1);
        assertThat(round.hasMorePhases()).isFalse();
        assertThat(round.wordSet()).isEqualTo(SECOND_WORD_SET);
        assertThat(round.turnsPlayed()).isEqualTo(0);
        assertThat(round.latestChainWord()).isEqualTo("Cama");
    }

    @Test
    void advancePhaseDoesNotClearAttemptsOrResetTurnRotation() {
        Round round = newRound(2);
        round.submitWord("alice", "Escribir", new WordJudgement(true, 80, null, 20, false));
        round.submitWord("bob", "Cama", new WordJudgement(true, 90, null, 100, true));

        round.advancePhase();

        assertThat(round.attempts()).hasSize(2);
        assertThat(round.attempts().get(0).phaseIndex()).isEqualTo(0);
        assertThat(round.attempts().get(1).phaseIndex()).isEqualTo(0);
        // rotation continues from carol (whoever was next), not reset to alice
        assertThat(round.currentTurnPlayerId()).isEqualTo("carol");

        round.submitWord("carol", "Playa", new WordJudgement(true, 70, null, 10, false));
        assertThat(round.attempts().get(2).phaseIndex()).isEqualTo(1);
    }

    @Test
    void advancePhaseThrowsWhenNoMorePhasesRemain() {
        Round round = newRound(1);

        assertThatThrownBy(round::advancePhase)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void loseToInfiltratorEndsTheGameAsALossWithoutVoting() {
        Round round = newRound(2);

        ChainResult result = round.loseToInfiltrator();

        assertThat(round.phase()).isEqualTo(RoundPhase.REVEAL);
        assertThat(round.phaseDeadline()).isNull();
        assertThat(result.crewWon()).isFalse();
        assertThat(result.endedByInfiltratorWord()).isTrue();
        assertThat(result.infiltratorPlayerIds()).containsExactly("carol");
        assertThat(result.infiltratorTargetWord()).isEqualTo("Océano");
        assertThat(result.accusedPlayerId()).isNull();
        assertThat(round.result()).isEqualTo(result);
    }

    @Test
    void loseToInfiltratorRequiresWordChainPhase() {
        Round round = newRound();
        round.startVoting(Instant.now().plusSeconds(30));

        assertThatThrownBy(round::loseToInfiltrator)
                .isInstanceOf(WrongPhaseException.class);
    }
}
