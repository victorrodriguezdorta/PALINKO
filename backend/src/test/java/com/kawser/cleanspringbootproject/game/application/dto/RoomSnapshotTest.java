package com.kawser.cleanspringbootproject.game.application.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kawser.cleanspringbootproject.game.domain.model.GameLanguage;
import com.kawser.cleanspringbootproject.game.domain.model.Player;
import com.kawser.cleanspringbootproject.game.domain.model.Room;
import com.kawser.cleanspringbootproject.game.domain.model.RoomSettings;
import com.kawser.cleanspringbootproject.game.domain.model.WordJudgement;
import com.kawser.cleanspringbootproject.game.domain.model.WordSet;
import com.kawser.cleanspringbootproject.game.domain.service.DefaultScoringPolicy;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The single most important safety net in this rewrite: RoomSnapshot is the
 * only place allowed to decide what a given viewer gets to see, and the
 * whole game concept depends on the infiltrator never finding out (via the
 * wire payload) that their target word differs from everyone else's before
 * REVEAL.
 */
class RoomSnapshotTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Room startedRoom() {
        return startedRoom(1);
    }

    private Room startedRoom(int phaseCount) {
        // 3 players (not 2): maxInfiltratorCount is floor(playerCount / 3),
        // so these infiltrator-focused assertions need at least 3 players
        // for the default infiltratorCount=1 to actually deal one in.
        RoomSettings settings = new RoomSettings(30, 30, GameLanguage.SPANISH, 1, phaseCount, false);
        Player host = Player.host("host-1", "token-1", "Host");
        Room room = Room.create("ABC123", settings, host, Instant.now());
        room.addPlayer(Player.guest("guest-1", "token-2", "Guest"), Instant.now());
        room.addPlayer(Player.guest("guest-2", "token-3", "Guest2"), Instant.now());
        List<WordSet> phaseWordSets = phaseCount == 1
                ? List.of(new WordSet("Bolígrafo", "Cama", "Océano"))
                : List.of(new WordSet("Bolígrafo", "Cama", "Océano"), new WordSet("Cama", "Fresa", "Coche"));
        room.start(phaseWordSets, Instant.now());
        return room;
    }

    private String toJson(RoomSnapshot snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void chainViewNeverExposesInfiltratorIdentityOrTargetBeforeReveal() {
        Room room = startedRoom();
        String infiltratorId = room.round().infiltratorPlayerIds().iterator().next();
        String infiltratorTarget = room.round().wordSet().infiltratorTargetWord();
        String nonInfiltratorViewerId = room.players().stream()
                .map(Player::id)
                .filter(id -> !id.equals(infiltratorId))
                .findFirst()
                .orElseThrow();

        RoomSnapshot snapshot = RoomSnapshot.from(room, nonInfiltratorViewerId);
        String json = toJson(snapshot);

        // The infiltrator's own id legitimately appears in the plain player
        // roster (and possibly as hostPlayerId) regardless of their secret
        // role — that is not a leak. What must never appear pre-REVEAL is
        // their differing target word, or a populated reveal section that
        // would name them as the infiltrator.
        assertThat(snapshot.chain().reveal()).isNull();
        assertThat(json).doesNotContain(infiltratorTarget);
        assertThat(json).doesNotContain("\"infiltratorPlayerIds\"");
        assertThat(snapshot.chain().yourTargetWord()).isNotEqualTo(infiltratorTarget);
    }

    @Test
    void everyViewerSeesTheirOwnTargetWordFromTheirOwnSnapshot() {
        Room room = startedRoom();

        for (Player player : room.players()) {
            RoomSnapshot snapshot = RoomSnapshot.from(room, player.id());
            assertThat(snapshot.chain().yourTargetWord()).isEqualTo(room.round().targetWordFor(player.id()));
        }
    }

    @Test
    void yourPhaseTargetWordsExposesTheWholePrecomputedChainWithoutLeakingTheOtherRolesWords() {
        Room room = startedRoom(2);
        String infiltratorId = room.round().infiltratorPlayerIds().iterator().next();
        String nonInfiltratorViewerId = room.players().stream()
                .map(Player::id)
                .filter(id -> !id.equals(infiltratorId))
                .findFirst()
                .orElseThrow();

        RoomSnapshot infiltratorSnapshot = RoomSnapshot.from(room, infiltratorId);
        RoomSnapshot crewSnapshot = RoomSnapshot.from(room, nonInfiltratorViewerId);

        // Every phase's start word is known up front, and each viewer's own
        // target chain matches their own role for every phase - but a crew
        // member's payload must never contain the infiltrator's differing
        // second-phase target ("Coche"), and vice versa.
        assertThat(crewSnapshot.chain().phaseStartWords()).containsExactly("Bolígrafo", "Cama");
        assertThat(crewSnapshot.chain().yourPhaseTargetWords()).containsExactly("Cama", "Fresa");
        assertThat(infiltratorSnapshot.chain().yourPhaseTargetWords()).containsExactly("Océano", "Coche");
        assertThat(toJson(crewSnapshot)).doesNotContain("Coche");
        assertThat(toJson(infiltratorSnapshot)).doesNotContain("\"Fresa\"");
    }

    @Test
    void revealExposesTheFullAcceptedWordChainAndPerPhaseWordCounts() {
        Room room = startedRoom(2);
        String infiltratorId = room.round().infiltratorPlayerIds().iterator().next();
        // Phase 1 ("Bolígrafo" -> "Cama"): two accepted words before
        // reaching the group target and advancing to phase 2.
        submitAccepted(room, "Escribir", false);
        submitAccepted(room, "Cama", true);
        room.round().advancePhase();
        // Phase 2 ("Cama" -> "Fresa"): just one accepted word.
        submitAccepted(room, "Fresa", true);
        room.round().startVoting(Instant.now().plusSeconds(1));
        room.revealRound(new DefaultScoringPolicy(), Instant.now());

        RoomSnapshot snapshot = RoomSnapshot.from(room, infiltratorId);

        assertThat(snapshot.chain().reveal().acceptedWordChain())
                .containsExactly("Escribir", "Cama", "Fresa");
        assertThat(snapshot.chain().reveal().acceptedWordCountByPhase())
                .containsExactly(2, 1);
    }

    private void submitAccepted(Room room, String text, boolean reachesTarget) {
        String playerId = room.round().currentTurnPlayerId();
        room.round().submitWord(playerId, text, new WordJudgement(true, 90, null, 20, reachesTarget));
    }

    @Test
    void revealExposesTheInfiltratorToEveryoneAfterTheVote() {
        Room room = startedRoom();
        String infiltratorId = room.round().infiltratorPlayerIds().iterator().next();
        room.round().startVoting(Instant.now().plusSeconds(1));
        room.revealRound(new DefaultScoringPolicy(), Instant.now());

        for (Player player : room.players()) {
            RoomSnapshot snapshot = RoomSnapshot.from(room, player.id());
            assertThat(snapshot.chain().reveal()).isNotNull();
            assertThat(snapshot.chain().reveal().infiltratorPlayerIds()).containsExactly(infiltratorId);
        }
    }
}
