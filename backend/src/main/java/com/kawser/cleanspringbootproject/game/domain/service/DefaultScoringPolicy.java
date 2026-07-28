package com.kawser.cleanspringbootproject.game.domain.service;

import com.kawser.cleanspringbootproject.game.domain.model.Answer;
import com.kawser.cleanspringbootproject.game.domain.model.Player;
import com.kawser.cleanspringbootproject.game.domain.model.Round;
import com.kawser.cleanspringbootproject.game.domain.model.RoundResult;
import com.kawser.cleanspringbootproject.game.domain.model.Vote;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Awards points for correctly spotting the AI's answer, and separate
 * "deceived someone" points to a human author whose answer was mistaken
 * for the AI's.
 */
public class DefaultScoringPolicy implements ScoringPolicy {

    public static final int POINTS_FOR_SPOTTING_AI = 100;
    public static final int POINTS_FOR_DECEIVING_A_VOTER = 50;

    @Override
    public RoundResult score(Round round, Collection<Player> players) {
        Answer aiAnswer = round.aiAnswer()
                .orElseThrow(() -> new IllegalStateException("Round has no AI answer to score against"));

        Map<String, Integer> deltas = new HashMap<>();
        for (Vote vote : round.votes()) {
            if (vote.answerId().equals(aiAnswer.id())) {
                deltas.merge(vote.voterPlayerId(), POINTS_FOR_SPOTTING_AI, Integer::sum);
            } else {
                findAuthorOf(round, vote.answerId())
                        .ifPresent(authorId -> deltas.merge(authorId, POINTS_FOR_DECEIVING_A_VOTER, Integer::sum));
            }
        }

        deltas.forEach((playerId, delta) -> players.stream()
                .filter(player -> player.id().equals(playerId))
                .findFirst()
                .ifPresent(player -> player.addScore(delta)));

        return new RoundResult(aiAnswer.id(), deltas);
    }

    private java.util.Optional<String> findAuthorOf(Round round, String answerId) {
        return round.answers().stream()
                .filter(answer -> answer.id().equals(answerId) && !answer.isAi())
                .map(Answer::authorPlayerId)
                .findFirst();
    }
}
