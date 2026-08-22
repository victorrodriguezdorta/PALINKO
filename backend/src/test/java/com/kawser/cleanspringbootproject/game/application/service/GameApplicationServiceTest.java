package com.kawser.cleanspringbootproject.game.application.service;

import com.kawser.cleanspringbootproject.game.application.dto.AdvancePhaseCommand;
import com.kawser.cleanspringbootproject.game.application.dto.CreateDailyRoomCommand;
import com.kawser.cleanspringbootproject.game.application.dto.CreateRoomCommand;
import com.kawser.cleanspringbootproject.game.application.dto.CreateRoomResult;
import com.kawser.cleanspringbootproject.game.application.dto.DisconnectCommand;
import com.kawser.cleanspringbootproject.game.application.dto.JoinRoomCommand;
import com.kawser.cleanspringbootproject.game.application.dto.JoinRoomResult;
import com.kawser.cleanspringbootproject.game.application.dto.KickPlayerCommand;
import com.kawser.cleanspringbootproject.game.application.dto.ReconnectCommand;
import com.kawser.cleanspringbootproject.game.application.dto.RewindWordCommand;
import com.kawser.cleanspringbootproject.game.application.dto.StartGameCommand;
import com.kawser.cleanspringbootproject.game.application.dto.SubmitWordCommand;
import com.kawser.cleanspringbootproject.game.application.dto.UpdateRoomSettingsCommand;
import com.kawser.cleanspringbootproject.game.application.port.out.ChainWordBank;
import com.kawser.cleanspringbootproject.game.application.port.out.DailyLeaderboardRecorder;
import com.kawser.cleanspringbootproject.game.application.port.out.DailyStreakTracker;
import com.kawser.cleanspringbootproject.game.application.port.out.PhaseScheduler;
import com.kawser.cleanspringbootproject.game.application.port.out.RoomCodeGenerator;
import com.kawser.cleanspringbootproject.game.application.port.out.RoomNotifier;
import com.kawser.cleanspringbootproject.game.application.port.out.RoomRepository;
import com.kawser.cleanspringbootproject.game.application.port.out.WordRelation;
import com.kawser.cleanspringbootproject.game.application.port.out.WordRelationChecker;
import com.kawser.cleanspringbootproject.game.application.port.out.WordSpellingCorrector;
import com.kawser.cleanspringbootproject.game.application.port.out.WordSubmissionRateLimiter;
import com.kawser.cleanspringbootproject.game.domain.exception.NoRewindAvailableException;
import com.kawser.cleanspringbootproject.game.domain.exception.NotHostException;
import com.kawser.cleanspringbootproject.game.domain.exception.NotYourTurnException;
import com.kawser.cleanspringbootproject.game.domain.exception.PlayerNotFoundException;
import com.kawser.cleanspringbootproject.game.domain.exception.RoomNotJoinableException;
import com.kawser.cleanspringbootproject.game.domain.model.GameLanguage;
import com.kawser.cleanspringbootproject.game.domain.model.Room;
import com.kawser.cleanspringbootproject.game.domain.model.RoomSettings;
import com.kawser.cleanspringbootproject.game.domain.model.RoomStatus;
import com.kawser.cleanspringbootproject.game.domain.model.Round;
import com.kawser.cleanspringbootproject.game.domain.model.RoundPhase;
import com.kawser.cleanspringbootproject.game.domain.model.WordSet;
import com.kawser.cleanspringbootproject.game.domain.service.DefaultScoringPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * End-to-end exercise of the whole chain lifecycle through the public use
 * cases, with WordRelationChecker mocked to deterministic values (never the
 * real random mock) so the outcome of every submitted word is predictable.
 */
class GameApplicationServiceTest {

    private static final WordSet WORD_SET = new WordSet("Bolígrafo", "Cama", "Océano");
    private static final WordSet SECOND_PHASE_WORD_SET = new WordSet("Cama", "Fresa", "Coche");

    private FakeRoomRepository roomRepository;
    private FakePhaseScheduler phaseScheduler;
    private WordRelationChecker wordRelationChecker;
    private FakeChainWordBank chainWordBank;
    private FakeDailyStreakTracker dailyStreakTracker;
    private FakeDailyLeaderboardRecorder dailyLeaderboardRecorder;
    private GameApplicationService service;
    private String roomCode;
    private String hostPlayerId;
    private String hostToken;
    private final Map<String, String> tokensByPlayerId = new HashMap<>();

    @BeforeEach
    void setUp() {
        roomRepository = new FakeRoomRepository();
        phaseScheduler = new FakePhaseScheduler();
        wordRelationChecker = mock(WordRelationChecker.class);
        RoomNotifier roomNotifier = mock(RoomNotifier.class);
        WordSpellingCorrector wordSpellingCorrector = (word, language) -> word;
        chainWordBank = new FakeChainWordBank();
        // A distinct code per call, not a fixed one: generateUniqueRoomCode()
        // retries against RoomRepository.existsByCode until it gets a fresh
        // code (see GameApplicationService), so a generator that always
        // returns the same value loops forever the moment a second room
        // (e.g. a daily room created after setUp()'s own room) needs a code.
        RoomCodeGenerator roomCodeGenerator = new RoomCodeGenerator() {
            private int counter;

            @Override
            public String generate() {
                return "ABC" + (counter++);
            }
        };

        WordSubmissionRateLimiter wordSubmissionRateLimiter = playerId -> true;
        dailyStreakTracker = new FakeDailyStreakTracker();
        dailyLeaderboardRecorder = new FakeDailyLeaderboardRecorder();

        service = new GameApplicationService(
                roomRepository, roomNotifier, wordRelationChecker, wordSpellingCorrector, chainWordBank,
                chainWordBank, roomCodeGenerator, phaseScheduler, new DefaultScoringPolicy(),
                wordSubmissionRateLimiter, dailyStreakTracker, dailyLeaderboardRecorder);

        CreateRoomResult created = service.createRoom(new CreateRoomCommand("Host", "seed-host", GameLanguage.SPANISH));
        roomCode = created.roomCode();
        hostPlayerId = created.playerId();
        hostToken = created.reconnectToken();
        tokensByPlayerId.put(hostPlayerId, hostToken);

        JoinRoomResult guestB = service.joinRoom(new JoinRoomCommand(roomCode, "GuestB", "seed-guestB"));
        tokensByPlayerId.put(guestB.playerId(), guestB.reconnectToken());
        JoinRoomResult guestC = service.joinRoom(new JoinRoomCommand(roomCode, "GuestC", "seed-guestC"));
        tokensByPlayerId.put(guestC.playerId(), guestC.reconnectToken());
    }

    private Room room() {
        return roomRepository.findByCode(roomCode).orElseThrow();
    }

    private void startGame() {
        service.startGame(new StartGameCommand(roomCode, hostPlayerId, hostToken));
    }

    /** Updates only phaseCount from the lobby, keeping every other setting at its default. */
    private void updateSettings(int phaseCount) {
        RoomSettings current = room().settings();
        service.updateRoomSettings(new UpdateRoomSettingsCommand(
                roomCode, hostPlayerId, hostToken,
                current.wordTimeSeconds(), current.voteTimeSeconds(), current.language(),
                current.infiltratorCount(), phaseCount));
    }

    private void submitWordAsCurrentPlayer(String wordText) {
        String playerId = room().round().currentTurnPlayerId();
        service.submitWord(new SubmitWordCommand(roomCode, playerId, tokensByPlayerId.get(playerId), wordText));
    }

    private void fireScheduledTimeout() {
        service.forceAdvance(new AdvancePhaseCommand(
                phaseScheduler.lastRoomCode, phaseScheduler.lastExpectedPhaseIndex,
                phaseScheduler.lastExpectedTurnsPlayed, phaseScheduler.lastExpectedPhase));
    }

    @Test
    void startGameDealsAWordChainInProgress() {
        startGame();

        Room room = room();
        assertThat(room.status()).isEqualTo(RoomStatus.IN_PROGRESS);
        assertThat(room.round().phase()).isEqualTo(RoundPhase.WORD_CHAIN);
        assertThat(room.round().latestChainWord()).isEqualTo("Bolígrafo");
        assertThat(phaseScheduler.lastExpectedPhase).isEqualTo(RoundPhase.WORD_CHAIN);
    }

    @Test
    void rejectedWordDoesNotAdvanceTheChainAndEarnsNoPoints() {
        startGame();
        String authorId = room().round().currentTurnPlayerId();
        when(wordRelationChecker.relatedness(eq("Elefante"), eq("Bolígrafo"), any(GameLanguage.class)))
                .thenReturn(new WordRelation(10, null));

        submitWordAsCurrentPlayer("Elefante");

        Round round = room().round();
        assertThat(round.latestChainWord()).isEqualTo("Bolígrafo");
        assertThat(round.turnsPlayed()).isEqualTo(1);
        assertThat(round.attempts().get(0).outcome().name()).isEqualTo("REJECTED");
        assertThat(room().findPlayer(authorId).orElseThrow().score()).isZero();
    }

    @Test
    void acceptedWordAdvancesTheChainAndAwardsPoints() {
        startGame();
        String authorId = room().round().currentTurnPlayerId();
        when(wordRelationChecker.relatedness(eq("Escribir"), eq("Bolígrafo"), any(GameLanguage.class)))
                .thenReturn(new WordRelation(90, null));
        // "Escribir" only coincidentally matching a target would also set
        // reachedTarget via string equality, not this stub; the fixed word
        // bank's targets ("Cama"/"Océano") never equal "Escribir".
        when(wordRelationChecker.relatedness(eq("Escribir"), eq("Cama"), any(GameLanguage.class)))
                .thenReturn(new WordRelation(20, null));
        when(wordRelationChecker.relatedness(eq("Escribir"), eq("Océano"), any(GameLanguage.class)))
                .thenReturn(new WordRelation(20, null));

        submitWordAsCurrentPlayer("Escribir");

        Round round = room().round();
        assertThat(round.latestChainWord()).isEqualTo("Escribir");
        assertThat(round.currentTurnPlayerId()).isNotEqualTo(authorId);
        assertThat(room().findPlayer(authorId).orElseThrow().score()).isEqualTo(90);
    }

    @Test
    void rewindUndoesTheLastAcceptedWordAndReversesItsAuthorsPoints() {
        startGame();
        String firstAuthor = room().round().currentTurnPlayerId();
        when(wordRelationChecker.relatedness(eq("Escribir"), eq("Bolígrafo"), any(GameLanguage.class)))
                .thenReturn(new WordRelation(90, null));
        when(wordRelationChecker.relatedness(anyString(), eq("Cama"), any(GameLanguage.class)))
                .thenReturn(new WordRelation(20, null));
        when(wordRelationChecker.relatedness(anyString(), eq("Océano"), any(GameLanguage.class)))
                .thenReturn(new WordRelation(20, null));
        submitWordAsCurrentPlayer("Escribir");
        assertThat(room().findPlayer(firstAuthor).orElseThrow().score()).isEqualTo(90);
        String currentTurnPlayerId = room().round().currentTurnPlayerId();

        service.rewindWord(new RewindWordCommand(roomCode, currentTurnPlayerId, tokensByPlayerId.get(currentTurnPlayerId)));

        Round round = room().round();
        assertThat(round.latestChainWord()).isEqualTo("Bolígrafo");
        assertThat(round.attempts()).isEmpty();
        assertThat(room().findPlayer(firstAuthor).orElseThrow().score()).isZero();
        // Rewinding doesn't hand the undone word's author their turn back —
        // it stays with whoever was already up next.
        assertThat(round.currentTurnPlayerId()).isEqualTo(currentTurnPlayerId);
    }

    @Test
    void onlyTheCurrentTurnPlayerCanRewind() {
        startGame();
        when(wordRelationChecker.relatedness(anyString(), anyString(), any(GameLanguage.class)))
                .thenReturn(new WordRelation(90, null));
        submitWordAsCurrentPlayer("Escribir");
        String notCurrentTurnPlayerId = tokensByPlayerId.keySet().stream()
                .filter(id -> !id.equals(room().round().currentTurnPlayerId()))
                .findFirst()
                .orElseThrow();

        assertThatThrownBy(() -> service.rewindWord(
                new RewindWordCommand(roomCode, notCurrentTurnPlayerId, tokensByPlayerId.get(notCurrentTurnPlayerId))))
                .isInstanceOf(NotYourTurnException.class);
    }

    @Test
    void rewindCanOnlyBeUsedOncePerPlayerForTheWholeGame() {
        startGame();
        when(wordRelationChecker.relatedness(anyString(), anyString(), any(GameLanguage.class)))
                .thenReturn(new WordRelation(90, null));
        submitWordAsCurrentPlayer("Escribir");
        String rewinderId = room().round().currentTurnPlayerId();
        service.rewindWord(new RewindWordCommand(roomCode, rewinderId, tokensByPlayerId.get(rewinderId)));
        // rewinderId's turn doesn't change from rewinding itself; play a
        // full lap of the turn order so it's their turn again before they
        // try (and fail) to spend the power a second time.
        submitWordAsCurrentPlayer("Libro");
        submitWordAsCurrentPlayer("Biblioteca");
        assertThat(room().round().currentTurnPlayerId()).isEqualTo(rewinderId);

        assertThatThrownBy(() -> service.rewindWord(
                new RewindWordCommand(roomCode, rewinderId, tokensByPlayerId.get(rewinderId))))
                .isInstanceOf(NoRewindAvailableException.class);
    }

    @Test
    void timeoutSkipsTheTurnWithoutPenaltyAndKeepsTheChainWord() {
        startGame();
        String skippedPlayerId = room().round().currentTurnPlayerId();

        fireScheduledTimeout();

        Room room = room();
        assertThat(room.round().attempts().get(0).outcome().name()).isEqualTo("SKIPPED");
        assertThat(room.round().latestChainWord()).isEqualTo("Bolígrafo");
        assertThat(room.round().currentTurnPlayerId()).isNotEqualTo(skippedPlayerId);
        assertThat(room.findPlayer(skippedPlayerId).orElseThrow().score()).isZero();
    }

    @Test
    void hostDisconnectingKeepsTheRoomAliveForOtherPlayers() {
        startGame();
        String hostId = room().hostPlayerId();

        service.handleDisconnect(new DisconnectCommand(roomCode, hostId));

        // A momentary host disconnect no longer tears the room down or
        // evicts everyone else — it's still there, just with the host
        // marked disconnected, so they (or the cleanup sweep after the
        // grace window) can decide what happens next.
        Room room = room();
        assertThat(room.status()).isEqualTo(RoomStatus.IN_PROGRESS);
        assertThat(room.findPlayer(hostId).orElseThrow().isConnected()).isFalse();
    }

    @Test
    void hostReconnectingAfterADisconnectClearsTheirDisconnectedFlag() {
        startGame();
        String hostId = room().hostPlayerId();
        service.handleDisconnect(new DisconnectCommand(roomCode, hostId));

        service.reconnect(new ReconnectCommand(roomCode, hostId, hostToken));

        Room room = room();
        assertThat(room.status()).isEqualTo(RoomStatus.IN_PROGRESS);
        assertThat(room.findPlayer(hostId).orElseThrow().isConnected()).isTrue();
        assertThat(room.isHostReconnectWindowExpired(Instant.now().plusSeconds(999_999), Duration.ofSeconds(120)))
                .isFalse();
    }

    @Test
    void hostCanKickAGuestFromTheLobby() {
        String guestId = tokensByPlayerId.keySet().stream().filter(id -> !id.equals(hostPlayerId)).findFirst().orElseThrow();

        service.kickPlayer(new KickPlayerCommand(roomCode, hostPlayerId, hostToken, guestId));

        assertThat(room().findPlayer(guestId)).isEmpty();
    }

    @Test
    void hostCanKickAGuestMidGameAndTheGuestCanNeverReconnect() {
        startGame();
        String guestId = tokensByPlayerId.keySet().stream()
                .filter(id -> !id.equals(hostPlayerId) && !id.equals(room().round().currentTurnPlayerId()))
                .findFirst()
                .orElseThrow();

        service.kickPlayer(new KickPlayerCommand(roomCode, hostPlayerId, hostToken, guestId));

        assertThat(room().status()).isEqualTo(RoomStatus.IN_PROGRESS);
        assertThat(room().findPlayer(guestId)).isPresent();
        assertThat(room().findPlayer(guestId).orElseThrow().isKicked()).isTrue();
        assertThat(room().round().turnOrder()).contains(guestId);
        assertThatThrownBy(() -> service.reconnect(
                new ReconnectCommand(roomCode, guestId, tokensByPlayerId.get(guestId))))
                .isInstanceOf(PlayerNotFoundException.class);
    }

    @Test
    void kickingThePlayerWhoseTurnItCurrentlyIsSkipsToTheNextPlayerImmediately() {
        startGame();
        String kickedPlayerId = room().round().currentTurnPlayerId();

        service.kickPlayer(new KickPlayerCommand(roomCode, hostPlayerId, hostToken, kickedPlayerId));

        // Mirrors handleDisconnect's own equivalent: nobody should be stuck
        // waiting out the kicked player's own turn timeout.
        assertThat(room().round().currentTurnPlayerId()).isNotEqualTo(kickedPlayerId);
    }

    @Test
    void nonHostCannotKickAnotherPlayer() {
        String guestId = tokensByPlayerId.keySet().stream().filter(id -> !id.equals(hostPlayerId)).findFirst().orElseThrow();
        String otherGuestId = tokensByPlayerId.keySet().stream()
                .filter(id -> !id.equals(hostPlayerId) && !id.equals(guestId))
                .findFirst()
                .orElseThrow();

        assertThatThrownBy(() -> service.kickPlayer(
                new KickPlayerCommand(roomCode, guestId, tokensByPlayerId.get(guestId), otherGuestId)))
                .isInstanceOf(NotHostException.class);
        assertThat(room().findPlayer(otherGuestId)).isPresent();
    }

    @Test
    void createDailyRoomStartsASoloCooperativeGameWithNoTimer() {
        CreateRoomResult created = service.createDailyRoom(new CreateDailyRoomCommand(GameLanguage.SPANISH, null));

        Room dailyRoom = roomRepository.findByCode(created.roomCode()).orElseThrow();
        assertThat(dailyRoom.status()).isEqualTo(RoomStatus.IN_PROGRESS);
        assertThat(dailyRoom.players()).hasSize(1);
        assertThat(dailyRoom.settings().daily()).isTrue();
        assertThat(dailyRoom.round().infiltratorPlayerIds()).isEmpty();
        assertThat(dailyRoom.round().phase()).isEqualTo(RoundPhase.WORD_CHAIN);
        // A daily room never arms a PhaseScheduler timeout after dealing
        // the first turn, unlike a standard room (see
        // startGameDealsAWordChainInProgress) — no deadline is ever set.
        assertThat(dailyRoom.round().phaseDeadline()).isNull();
    }

    @Test
    void createDailyRoomNamesTheSoloPlayerAfterTheAuthenticatedUsernameWhenPresent() {
        CreateRoomResult created = service.createDailyRoom(new CreateDailyRoomCommand(GameLanguage.SPANISH, "someUser"));

        Room dailyRoom = roomRepository.findByCode(created.roomCode()).orElseThrow();
        assertThat(dailyRoom.players()).extracting("name").containsExactly("someUser");
    }

    @Test
    void createDailyRoomNeverSchedulesATimeoutAfterASubmittedWordEither() {
        CreateRoomResult created = service.createDailyRoom(new CreateDailyRoomCommand(GameLanguage.SPANISH, null));
        String soloPlayerId = created.playerId();
        when(wordRelationChecker.relatedness(anyString(), anyString(), any(GameLanguage.class)))
                .thenReturn(new WordRelation(10, null));

        service.submitWord(new SubmitWordCommand(
                created.roomCode(), soloPlayerId, created.reconnectToken(), "Filler"));

        Room dailyRoom = roomRepository.findByCode(created.roomCode()).orElseThrow();
        assertThat(dailyRoom.round().phaseDeadline()).isNull();
    }

    @Test
    void secondPlayerCannotJoinADailyRoom() {
        CreateRoomResult created = service.createDailyRoom(new CreateDailyRoomCommand(GameLanguage.SPANISH, null));

        assertThatThrownBy(() -> service.joinRoom(new JoinRoomCommand(created.roomCode(), "Intruder", "seed-intruder")))
                .isInstanceOf(RoomNotJoinableException.class);
    }

    @Test
    void completingTheDailyChallengeRecordsTheStreakForALoggedInPlayer() {
        CreateRoomResult created = service.createDailyRoom(new CreateDailyRoomCommand(GameLanguage.SPANISH, "someUser"));
        when(wordRelationChecker.relatedness(anyString(), anyString(), any(GameLanguage.class)))
                .thenReturn(new WordRelation(95, null));

        // FakeChainWordBank's nextPhaseWordSet always returns
        // SECOND_PHASE_WORD_SET ("Cama" -> "Fresa"/"Coche") regardless of
        // phase count, so both the daily room's phase 2 and phase 3 share
        // that same group target "Fresa" — submitting it twice (once per
        // phase) drives the whole 3-phase chain through to completion.
        // "Coche" is deliberately never submitted here: it's that WordSet's
        // infiltrator target, and with no infiltrators in this room (see
        // Room.maxInfiltratorCount), any non-infiltrator writing it ends the
        // whole game immediately as a loss instead of advancing the phase.
        service.submitWord(new SubmitWordCommand(created.roomCode(), created.playerId(), created.reconnectToken(), "Cama"));
        service.submitWord(new SubmitWordCommand(created.roomCode(), created.playerId(), created.reconnectToken(), "Fresa"));
        service.submitWord(new SubmitWordCommand(created.roomCode(), created.playerId(), created.reconnectToken(), "Fresa"));

        Room dailyRoom = roomRepository.findByCode(created.roomCode()).orElseThrow();
        assertThat(dailyRoom.status()).isEqualTo(RoomStatus.FINISHED);
        assertThat(dailyStreakTracker.recordedUsernames).containsExactly("someUser");
        // 3 accepted words ("Cama", "Fresa", "Fresa"), each stubbed at 95%
        // relatedness-to-previous.
        assertThat(dailyLeaderboardRecorder.recordedUsernames).containsExactly("someUser");
        assertThat(dailyLeaderboardRecorder.lastWordCount).isEqualTo(3);
        assertThat(dailyLeaderboardRecorder.lastAccuracyPercent).isEqualTo(95);
    }

    @Test
    void completingTheDailyChallengeAsAGuestNeverRecordsAStreakOrALeaderboardResult() {
        CreateRoomResult created = service.createDailyRoom(new CreateDailyRoomCommand(GameLanguage.SPANISH, null));
        when(wordRelationChecker.relatedness(anyString(), anyString(), any(GameLanguage.class)))
                .thenReturn(new WordRelation(95, null));

        service.submitWord(new SubmitWordCommand(created.roomCode(), created.playerId(), created.reconnectToken(), "Cama"));
        service.submitWord(new SubmitWordCommand(created.roomCode(), created.playerId(), created.reconnectToken(), "Fresa"));
        service.submitWord(new SubmitWordCommand(created.roomCode(), created.playerId(), created.reconnectToken(), "Fresa"));

        Room dailyRoom = roomRepository.findByCode(created.roomCode()).orElseThrow();
        assertThat(dailyRoom.status()).isEqualTo(RoomStatus.FINISHED);
        assertThat(dailyStreakTracker.recordedUsernames).isEmpty();
        assertThat(dailyLeaderboardRecorder.recordedUsernames).isEmpty();
    }

    @Test
    void twoDailyRoomsCreatedForTheSameLanguageDealTheIdenticalChain() {
        // The fake ChainWordBank ignores its seed parameter (ChainWordBank's
        // own default method), so both calls return the very same fixed
        // WORD_SET regardless — this test instead pins down the contract
        // GameApplicationService must uphold: createDailyRoom always calls
        // through fullChain(language, phaseCount, seed) rather than the
        // unseeded overload, which real adapters rely on for determinism.
        CreateRoomResult first = service.createDailyRoom(new CreateDailyRoomCommand(GameLanguage.SPANISH, null));
        CreateRoomResult second = service.createDailyRoom(new CreateDailyRoomCommand(GameLanguage.SPANISH, null));

        Room firstRoom = roomRepository.findByCode(first.roomCode()).orElseThrow();
        Room secondRoom = roomRepository.findByCode(second.roomCode()).orElseThrow();
        assertThat(firstRoom.round().phaseWordSets()).isEqualTo(secondRoom.round().phaseWordSets());
    }

    @Test
    void reachingTargetOnANonFinalPhaseAdvancesToNextPhaseInsteadOfStartingVoting() {
        updateSettings(2);
        startGame();
        when(wordRelationChecker.relatedness(anyString(), eq("Bolígrafo"), any(GameLanguage.class)))
                .thenReturn(new WordRelation(10, null));
        when(wordRelationChecker.relatedness(eq("Cama"), anyString(), any(GameLanguage.class)))
                .thenReturn(new WordRelation(95, null));

        String firstAuthor = room().round().currentTurnPlayerId();
        submitWordAsCurrentPlayer("Cama");

        Round round = room().round();
        assertThat(round.phase()).isEqualTo(RoundPhase.WORD_CHAIN);
        assertThat(round.phaseIndex()).isEqualTo(1);
        assertThat(round.hasMorePhases()).isFalse();
        assertThat(round.wordSet()).isEqualTo(SECOND_PHASE_WORD_SET);
        assertThat(round.latestChainWord()).isEqualTo("Cama");
        assertThat(round.turnsPlayed()).isEqualTo(0);
        assertThat(phaseScheduler.lastExpectedPhase).isEqualTo(RoundPhase.WORD_CHAIN);
        assertThat(phaseScheduler.lastExpectedPhaseIndex).isEqualTo(1);
        // rotation continues forward rather than resetting to the phase-1 opener
        assertThat(round.currentTurnPlayerId()).isNotEqualTo(firstAuthor);
    }

    @Test
    void wordChainNeverForceEndsFromTurnCountAloneNoMatterHowManyTimeoutsElapse() {
        startGame();
        when(wordRelationChecker.relatedness(anyString(), anyString(), any(GameLanguage.class))).thenReturn(new WordRelation(10, null));

        for (int i = 0; i < 50; i++) {
            fireScheduledTimeout();
        }

        Round round = room().round();
        assertThat(round.phase()).isEqualTo(RoundPhase.WORD_CHAIN);
        assertThat(round.phaseIndex()).isEqualTo(0);
        assertThat(round.turnsPlayed()).isEqualTo(50);
    }

    @Test
    void staleForceAdvanceWithOutdatedPhaseIndexIsANoOp() {
        updateSettings(2);
        startGame();
        when(wordRelationChecker.relatedness(anyString(), anyString(), any(GameLanguage.class))).thenReturn(new WordRelation(10, null));
        when(wordRelationChecker.relatedness(eq("Cama"), anyString(), any(GameLanguage.class)))
                .thenReturn(new WordRelation(95, null));
        String staleRoomCode = roomCode;
        int staleExpectedTurnsPlayed = room().round().turnsPlayed();
        RoundPhase staleExpectedPhase = room().round().phase();
        int stalePhaseIndex = room().round().phaseIndex();

        submitWordAsCurrentPlayer("Cama"); // advances to phase 2, phaseIndex now 1

        service.forceAdvance(new AdvancePhaseCommand(
                staleRoomCode, stalePhaseIndex, staleExpectedTurnsPlayed, staleExpectedPhase));

        Round round = room().round();
        assertThat(round.phaseIndex()).isEqualTo(1);
        assertThat(round.phase()).isEqualTo(RoundPhase.WORD_CHAIN);
        assertThat(round.turnsPlayed()).isEqualTo(0);
    }

    private static class FakeRoomRepository implements RoomRepository {
        private final Map<String, Room> rooms = new HashMap<>();

        @Override
        public Optional<Room> findByCode(String code) {
            return Optional.ofNullable(rooms.get(code));
        }

        @Override
        public boolean existsByCode(String code) {
            return rooms.containsKey(code);
        }

        @Override
        public void save(Room room) {
            rooms.put(room.code(), room);
        }

        @Override
        public void deleteByCode(String code) {
            rooms.remove(code);
        }

        @Override
        public List<Room> findAll() {
            return List.copyOf(rooms.values());
        }

        @Override
        public <T> T mutate(String code, Function<Room, T> action) {
            return action.apply(rooms.get(code));
        }
    }

    private static class FakeChainWordBank implements ChainWordBank {
        WordSet nextPhaseWordSetToReturn = SECOND_PHASE_WORD_SET;

        @Override
        public WordSet firstWordSet(GameLanguage language) {
            return WORD_SET;
        }

        @Override
        public WordSet nextPhaseWordSet(GameLanguage language, String startWord, Set<String> usedWords) {
            return nextPhaseWordSetToReturn;
        }
    }

    private static class FakeDailyStreakTracker implements DailyStreakTracker {
        final List<String> recordedUsernames = new java.util.ArrayList<>();

        @Override
        public void recordDailyCompletion(String username) {
            recordedUsernames.add(username);
        }
    }

    private static class FakeDailyLeaderboardRecorder implements DailyLeaderboardRecorder {
        final List<String> recordedUsernames = new java.util.ArrayList<>();
        Integer lastWordCount;
        Integer lastAccuracyPercent;

        @Override
        public void recordResult(String username, int wordCount, int accuracyPercent) {
            recordedUsernames.add(username);
            lastWordCount = wordCount;
            lastAccuracyPercent = accuracyPercent;
        }
    }

    private static class FakePhaseScheduler implements PhaseScheduler {
        String lastRoomCode;
        int lastExpectedPhaseIndex;
        int lastExpectedTurnsPlayed;
        RoundPhase lastExpectedPhase;

        @Override
        public void scheduleAdvance(
                String roomCode, int expectedPhaseIndex, int expectedTurnsPlayed, RoundPhase expectedPhase,
                Instant fireAt) {
            this.lastRoomCode = roomCode;
            this.lastExpectedPhaseIndex = expectedPhaseIndex;
            this.lastExpectedTurnsPlayed = expectedTurnsPlayed;
            this.lastExpectedPhase = expectedPhase;
        }

        @Override
        public void cancel(String roomCode) {
            // no-op: nothing pending to cancel in these synchronous tests
        }
    }
}
