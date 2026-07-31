package com.kawser.cleanspringbootproject.game.domain.service;

import com.kawser.cleanspringbootproject.game.domain.model.ChainResult;
import com.kawser.cleanspringbootproject.game.domain.model.Player;
import com.kawser.cleanspringbootproject.game.domain.model.Round;
import com.kawser.cleanspringbootproject.game.domain.model.Vote;
import com.kawser.cleanspringbootproject.game.domain.model.WordSet;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultScoringPolicyTest {

    private static final List<String> TURN_ORDER = List.of("alice", "bob", "carol");
    private static final WordSet WORD_SET = new WordSet("Bolígrafo", "Cama", "Océano");

    private final DefaultScoringPolicy policy = new DefaultScoringPolicy();

    @Test
    void rejectedWordNeverCostsPointsRegardlessOfRelatednessOrTargetBonus() {
        assertThat(policy.scoreWordAttempt(false, 90, false)).isZero();
        assertThat(policy.scoreWordAttempt(false, 90, true)).isZero();
        assertThat(policy.scoreWordAttempt(false, 0, false)).isZero();
    }

    @Test
    void acceptedWordWithoutTargetBonusEarnsExactlyItsRelatednessPercentage() {
        assertThat(policy.scoreWordAttempt(true, 72, false)).isEqualTo(72);
    }

    @Test
    void acceptedWordWithTargetBonusEarnsRelatednessPlusTheBonus() {
        assertThat(policy.scoreWordAttempt(true, 72, true))
                .isEqualTo(72 + DefaultScoringPolicy.POINTS_TARGET_BONUS);
    }

    private Round votingRound(String infiltratorPlayerId) {
        Round round = new Round(List.of(WORD_SET), TURN_ORDER, Set.of(infiltratorPlayerId), 1);
        round.startVoting(Instant.now().plusSeconds(30));
        return round;
    }

    private List<Player> players() {
        return List.of(
                Player.host("alice", "t1", "Alice", "seed-alice"),
                Player.guest("bob", "t2", "Bob", "seed-bob"),
                Player.guest("carol", "t3", "Carol", "seed-carol"));
    }

    @Test
    void crewWinsWhenTheOnlyMostAccusedPlayerIsTheInfiltrator() {
        Round round = votingRound("carol");
        round.submitVote(new Vote("alice", "carol"));
        round.submitVote(new Vote("bob", "carol"));

        ChainResult result = policy.scoreAccusation(round, players());

        assertThat(result.crewWon()).isTrue();
        assertThat(result.accusedPlayerId()).isEqualTo("carol");
        assertThat(result.scoreDeltaByPlayerId())
                .containsEntry("alice", DefaultScoringPolicy.POINTS_CREW_BONUS)
                .containsEntry("bob", DefaultScoringPolicy.POINTS_CREW_BONUS)
                .doesNotContainKey("carol");
    }

    @Test
    void infiltratorEscapesWhenAccusedIsWrong() {
        Round round = votingRound("carol");
        round.submitVote(new Vote("bob", "alice"));
        round.submitVote(new Vote("carol", "alice"));

        ChainResult result = policy.scoreAccusation(round, players());

        assertThat(result.crewWon()).isFalse();
        assertThat(result.accusedPlayerId()).isEqualTo("alice");
        assertThat(result.scoreDeltaByPlayerId())
                .containsEntry("carol", DefaultScoringPolicy.POINTS_INFILTRATOR_ESCAPE_BONUS)
                .hasSize(1);
    }

    @Test
    void infiltratorEscapesOnATieForMostAccused() {
        Round round = votingRound("carol");
        round.submitVote(new Vote("alice", "bob"));
        round.submitVote(new Vote("bob", "alice"));

        ChainResult result = policy.scoreAccusation(round, players());

        assertThat(result.crewWon()).isFalse();
        assertThat(result.accusedPlayerId()).isNull();
        assertThat(result.scoreDeltaByPlayerId()).containsEntry("carol", DefaultScoringPolicy.POINTS_INFILTRATOR_ESCAPE_BONUS);
    }

    @Test
    void infiltratorsOwnVoteIsExcludedFromTheTally() {
        Round round = votingRound("carol");
        round.submitVote(new Vote("alice", "carol"));
        round.submitVote(new Vote("carol", "bob"));

        ChainResult result = policy.scoreAccusation(round, players());

        // Without excluding carol's own vote this would be a 1-1 tie
        // between "carol" and "bob", letting the infiltrator dodge
        // conviction just by muddying their own vote count — it must not
        // count toward the tally at all, even though it is still cast and
        // still visible in the snapshot.
        assertThat(result.crewWon()).isTrue();
        assertThat(result.accusedPlayerId()).isEqualTo("carol");
    }

    @Test
    void infiltratorEscapesWhenNobodyVoted() {
        Round round = votingRound("carol");

        ChainResult result = policy.scoreAccusation(round, players());

        assertThat(result.crewWon()).isFalse();
        assertThat(result.accusedPlayerId()).isNull();
        assertThat(result.scoreDeltaByPlayerId()).containsEntry("carol", DefaultScoringPolicy.POINTS_INFILTRATOR_ESCAPE_BONUS);
    }
}
