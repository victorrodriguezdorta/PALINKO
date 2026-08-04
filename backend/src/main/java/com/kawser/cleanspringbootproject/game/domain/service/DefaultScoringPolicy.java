package com.kawser.cleanspringbootproject.game.domain.service;

import com.kawser.cleanspringbootproject.game.domain.model.ChainResult;
import com.kawser.cleanspringbootproject.game.domain.model.Player;
import com.kawser.cleanspringbootproject.game.domain.model.Round;
import com.kawser.cleanspringbootproject.game.domain.model.Vote;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A rejected word never costs points — it simply earns none. An accepted
 * word's payout scales with the AI's relatedness percentage against the
 * previous chain word, rather than a flat amount, so a barely-passing word
 * earns little and a strongly related one earns close to the full 100.
 */
public class DefaultScoringPolicy implements ScoringPolicy {

    public static final int RELATEDNESS_THRESHOLD = 40;
    public static final int POINTS_TARGET_BONUS = 15;
    public static final int POINTS_CREW_BONUS = 50;
    public static final int POINTS_INFILTRATOR_ESCAPE_BONUS = 75;

    @Override
    public int relatednessThreshold() {
        return RELATEDNESS_THRESHOLD;
    }

    @Override
    public int scoreWordAttempt(boolean accepted, int relatednessToPrevious, boolean metTargetBonus) {
        if (!accepted) {
            return 0;
        }
        return relatednessToPrevious + (metTargetBonus ? POINTS_TARGET_BONUS : 0);
    }

    /**
     * The most-accused player wins the vote for the crew only if there is a
     * single, unambiguous leader and it is any one of the (possibly
     * several) actual infiltrators — catching just one is enough, the rest
     * simply stay unrevealed; any tie for the top spot, a wrong leader, or
     * zero votes cast all resolve in the infiltrators' favor.
     *
     * Every infiltrator's own accusation is excluded from this tally — they
     * are still free to cast (and change) one like anybody else, and it
     * still shows up in RoomSnapshot same as every other vote, but it must
     * not be able to help the crew reach a majority against a third party,
     * or hide behind a self-inflicted tie. This method is only ever called
     * when there's at least one infiltrator — a 0-infiltrator room finishes
     * cooperatively instead (see Round.finishCooperatively).
     */
    @Override
    public ChainResult scoreAccusation(Round round, Collection<Player> players) {
        Set<String> infiltratorPlayerIds = round.infiltratorPlayerIds();
        Map<String, Long> tally = round.votes().stream()
                .filter(vote -> !infiltratorPlayerIds.contains(vote.voterPlayerId()))
                .collect(Collectors.groupingBy(Vote::suspectPlayerId, Collectors.counting()));

        long max = tally.values().stream().mapToLong(Long::longValue).max().orElse(0L);
        List<String> topSuspects = tally.entrySet().stream()
                .filter(entry -> entry.getValue() == max)
                .map(Map.Entry::getKey)
                .toList();

        boolean crewWon = max > 0 && topSuspects.size() == 1 && infiltratorPlayerIds.contains(topSuspects.get(0));
        String accusedPlayerId = topSuspects.size() == 1 ? topSuspects.get(0) : null;

        Map<String, Integer> deltas = new HashMap<>();
        if (crewWon) {
            players.stream()
                    .filter(player -> !infiltratorPlayerIds.contains(player.id()))
                    .forEach(player -> deltas.put(player.id(), POINTS_CREW_BONUS));
        } else {
            infiltratorPlayerIds.forEach(id -> deltas.put(id, POINTS_INFILTRATOR_ESCAPE_BONUS));
        }

        deltas.forEach((playerId, delta) -> players.stream()
                .filter(player -> player.id().equals(playerId))
                .findFirst()
                .ifPresent(player -> player.addScore(delta)));

        return new ChainResult(
                infiltratorPlayerIds, round.wordSet().infiltratorTargetWord(), accusedPlayerId, crewWon, deltas, false);
    }
}
