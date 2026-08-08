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
import com.kawser.cleanspringbootproject.game.application.dto.ResetRoomCommand;
import com.kawser.cleanspringbootproject.game.application.dto.RewindWordCommand;
import com.kawser.cleanspringbootproject.game.application.dto.RoomSnapshot;
import com.kawser.cleanspringbootproject.game.application.dto.StartGameCommand;
import com.kawser.cleanspringbootproject.game.application.dto.SubmitVoteCommand;
import com.kawser.cleanspringbootproject.game.application.dto.SubmitWordCommand;
import com.kawser.cleanspringbootproject.game.application.dto.UpdateRoomSettingsCommand;
import com.kawser.cleanspringbootproject.game.application.port.in.AdvancePhaseUseCase;
import com.kawser.cleanspringbootproject.game.application.port.in.CreateDailyRoomUseCase;
import com.kawser.cleanspringbootproject.game.application.port.in.CreateRoomUseCase;
import com.kawser.cleanspringbootproject.game.application.port.in.HandleDisconnectUseCase;
import com.kawser.cleanspringbootproject.game.application.port.in.JoinRoomUseCase;
import com.kawser.cleanspringbootproject.game.application.port.in.KickPlayerUseCase;
import com.kawser.cleanspringbootproject.game.application.port.in.ReconnectPlayerUseCase;
import com.kawser.cleanspringbootproject.game.application.port.in.ResetRoomUseCase;
import com.kawser.cleanspringbootproject.game.application.port.in.RewindWordUseCase;
import com.kawser.cleanspringbootproject.game.application.port.in.StartGameUseCase;
import com.kawser.cleanspringbootproject.game.application.port.in.SubmitVoteUseCase;
import com.kawser.cleanspringbootproject.game.application.port.in.SubmitWordUseCase;
import com.kawser.cleanspringbootproject.game.application.port.in.UpdateRoomSettingsUseCase;
import com.kawser.cleanspringbootproject.game.application.port.out.ChainWordBank;
import com.kawser.cleanspringbootproject.game.application.port.out.PhaseScheduler;
import com.kawser.cleanspringbootproject.game.application.port.out.RoomCodeGenerator;
import com.kawser.cleanspringbootproject.game.application.port.out.RoomNotifier;
import com.kawser.cleanspringbootproject.game.application.port.out.RoomRepository;
import com.kawser.cleanspringbootproject.game.application.port.out.WordRelation;
import com.kawser.cleanspringbootproject.game.application.port.out.WordRelationCheckException;
import com.kawser.cleanspringbootproject.game.application.port.out.WordRelationChecker;
import com.kawser.cleanspringbootproject.game.application.port.out.WordSpellingCorrector;
import com.kawser.cleanspringbootproject.game.application.port.out.WordSubmissionRateLimiter;
import com.kawser.cleanspringbootproject.game.domain.exception.PlayerNotFoundException;
import com.kawser.cleanspringbootproject.game.domain.exception.WordComparisonFailedException;
import com.kawser.cleanspringbootproject.game.domain.exception.WordSubmissionRateLimitedException;
import com.kawser.cleanspringbootproject.game.domain.model.ChainAttempt;
import com.kawser.cleanspringbootproject.game.domain.model.GameLanguage;
import com.kawser.cleanspringbootproject.game.domain.model.Player;
import com.kawser.cleanspringbootproject.game.domain.model.Room;
import com.kawser.cleanspringbootproject.game.domain.model.RoomSettings;
import com.kawser.cleanspringbootproject.game.domain.model.RoomStatus;
import com.kawser.cleanspringbootproject.game.domain.model.Round;
import com.kawser.cleanspringbootproject.game.domain.model.RoundPhase;
import com.kawser.cleanspringbootproject.game.domain.model.Vote;
import com.kawser.cleanspringbootproject.game.domain.model.WordJudgement;
import com.kawser.cleanspringbootproject.game.domain.model.WordSet;
import com.kawser.cleanspringbootproject.game.domain.service.ScoringPolicy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Orchestrates every use case for the game module. Kept as a single class
 * (rather than one implementation class per port.in interface) because all
 * of them share the same collaborators and the same per-room locking
 * discipline — splitting them would multiply boilerplate without adding any
 * real substitutability.
 */
public class GameApplicationService implements
        CreateRoomUseCase,
        CreateDailyRoomUseCase,
        JoinRoomUseCase,
        KickPlayerUseCase,
        ReconnectPlayerUseCase,
        HandleDisconnectUseCase,
        StartGameUseCase,
        SubmitWordUseCase,
        SubmitVoteUseCase,
        RewindWordUseCase,
        AdvancePhaseUseCase,
        ResetRoomUseCase,
        UpdateRoomSettingsUseCase {

    private static final int DAILY_PHASE_COUNT = 3;
    private static final String DAILY_PLAYER_NAME = "#";
    private static final String DAILY_PLAYER_AVATAR_SEED = "daily-challenge";

    private final RoomRepository roomRepository;
    private final RoomNotifier roomNotifier;
    private final WordRelationChecker wordRelationChecker;
    private final WordSpellingCorrector wordSpellingCorrector;
    private final ChainWordBank chainWordBank;
    private final ChainWordBank dailyChainWordBank;
    private final RoomCodeGenerator roomCodeGenerator;
    private final PhaseScheduler phaseScheduler;
    private final ScoringPolicy scoringPolicy;
    private final WordSubmissionRateLimiter wordSubmissionRateLimiter;

    public GameApplicationService(
            RoomRepository roomRepository,
            RoomNotifier roomNotifier,
            WordRelationChecker wordRelationChecker,
            WordSpellingCorrector wordSpellingCorrector,
            ChainWordBank chainWordBank,
            ChainWordBank dailyChainWordBank,
            RoomCodeGenerator roomCodeGenerator,
            PhaseScheduler phaseScheduler,
            ScoringPolicy scoringPolicy,
            WordSubmissionRateLimiter wordSubmissionRateLimiter) {
        this.roomRepository = roomRepository;
        this.roomNotifier = roomNotifier;
        this.wordRelationChecker = wordRelationChecker;
        this.wordSpellingCorrector = wordSpellingCorrector;
        this.chainWordBank = chainWordBank;
        this.dailyChainWordBank = dailyChainWordBank;
        this.roomCodeGenerator = roomCodeGenerator;
        this.phaseScheduler = phaseScheduler;
        this.scoringPolicy = scoringPolicy;
        this.wordSubmissionRateLimiter = wordSubmissionRateLimiter;
    }

    @Override
    public CreateRoomResult createRoom(CreateRoomCommand command) {
        String code = generateUniqueRoomCode();
        String hostId = UUID.randomUUID().toString();
        String hostToken = UUID.randomUUID().toString();
        Player host = Player.host(hostId, hostToken, command.hostName(), command.avatarSeed());

        Instant now = Instant.now();
        Room room = Room.create(code, RoomSettings.defaults(command.language()), host, now);
        roomRepository.save(room);

        RoomSnapshot snapshot = RoomSnapshot.from(room, hostId);
        return new CreateRoomResult(code, hostId, hostToken, snapshot);
    }

    /**
     * A daily challenge is a solo Room that starts already IN_PROGRESS: its
     * whole chain is dealt deterministically from today's UTC date (see
     * DailySeed), so every player who opens the challenge on the same date
     * plays the identical phases, in the same order, with the same words —
     * there is no LOBBY to sit in, no settings to tune, and (per
     * scheduleWordTurnTimeout below) no per-turn timer to race against.
     */
    @Override
    public CreateRoomResult createDailyRoom(CreateDailyRoomCommand command) {
        String code = generateUniqueRoomCode();
        String hostId = UUID.randomUUID().toString();
        String hostToken = UUID.randomUUID().toString();
        // A solo daily room has nobody else to show a name (or an avatar)
        // to, so the player never picks either — every daily room's sole
        // player is named "#" and given a fixed avatar seed rather than
        // taking arbitrary client input here.
        Player host = Player.host(hostId, hostToken, DAILY_PLAYER_NAME, DAILY_PLAYER_AVATAR_SEED);

        long seed = DailySeed.seedFor(DailySeed.today(), command.language());
        List<WordSet> phaseWordSets =
                dailyChainWordBank.fullChain(command.language(), DAILY_PHASE_COUNT, seed);

        Instant now = Instant.now();
        Room room = Room.create(code, RoomSettings.daily(command.language(), DAILY_PHASE_COUNT), host, now);
        room.start(phaseWordSets, now);
        endWordChainOrScheduleNextTurn(room);
        roomRepository.save(room);

        RoomSnapshot snapshot = RoomSnapshot.from(room, hostId);
        return new CreateRoomResult(code, hostId, hostToken, snapshot);
    }

    @Override
    public JoinRoomResult joinRoom(JoinRoomCommand command) {
        String playerId = UUID.randomUUID().toString();
        String reconnectToken = UUID.randomUUID().toString();
        Player guest = Player.guest(playerId, reconnectToken, command.playerName(), command.avatarSeed());

        Room room = roomRepository.mutate(command.roomCode(), r -> {
            r.addPlayer(guest, Instant.now());
            return r;
        });

        roomNotifier.notifyRoomUpdated(room);
        return new JoinRoomResult(playerId, reconnectToken, RoomSnapshot.from(room, playerId));
    }

    @Override
    public RoomSnapshot reconnect(ReconnectCommand command) {
        Room room = roomRepository.mutate(command.roomCode(), r -> {
            requireValidToken(r, command.playerId(), command.reconnectToken());
            r.markReconnected(command.playerId(), Instant.now());
            return r;
        });
        roomNotifier.notifyRoomUpdated(room);
        return RoomSnapshot.from(room, command.playerId());
    }

    @Override
    public void handleDisconnect(DisconnectCommand command) {
        // The host disconnecting no longer tears the room down immediately —
        // a momentary network blip shouldn't evict every other player. The
        // host is marked disconnected exactly like any other player, and the
        // room is only closed if they fail to reconnect within the cleanup
        // sweep's host grace window (see RoomCleanupTask).
        Room room = roomRepository.mutate(command.roomCode(), r -> {
            r.markDisconnected(command.playerId(), Instant.now());
            // If it was this player's turn, don't make everyone wait out
            // the full word-time timeout for someone who just left —
            // cancel it and immediately re-derive whose turn it is (or
            // end the chain), the same way the timeout itself would.
            if (r.status() == RoomStatus.IN_PROGRESS && r.hasRound()
                    && r.round().phase() == RoundPhase.WORD_CHAIN
                    && r.round().currentTurnPlayerId().equals(command.playerId())) {
                phaseScheduler.cancel(r.code());
                endWordChainOrScheduleNextTurn(r);
            }
            return r;
        });
        roomNotifier.notifyRoomUpdated(room);
    }

    @Override
    public void startGame(StartGameCommand command) {
        Room room = roomRepository.mutate(command.roomCode(), r -> {
            requireValidToken(r, command.playerId(), command.reconnectToken());
            r.requireHost(command.playerId());

            List<WordSet> phaseWordSets =
                    chainWordBank.fullChain(r.settings().language(), r.settings().phaseCount());
            r.start(phaseWordSets, Instant.now());
            endWordChainOrScheduleNextTurn(r);
            return r;
        });
        roomNotifier.notifyRoomUpdated(room);
    }

    @Override
    public void submitWord(SubmitWordCommand command) {
        if (command.wordText() == null || command.wordText().isBlank()) {
            throw new IllegalArgumentException("wordText must not be blank");
        }
        // Checked before touching the room lock or spending any AI call:
        // Round already allows only one submission per turn, but nothing
        // else stops a player from immediately re-triggering a fresh turn
        // in a single-player room (see daily challenges).
        if (!wordSubmissionRateLimiter.tryAcquire(command.playerId())) {
            throw new WordSubmissionRateLimitedException();
        }
        Room room = roomRepository.mutate(command.roomCode(), r -> {
            requireValidToken(r, command.playerId(), command.reconnectToken());
            Round round = r.round();
            round.requireCurrentTurn(command.playerId());

            GameLanguage language = r.settings().language();
            // Correct spelling first so a typo like "porfesor" is judged
            // (and stored in the chain) as "profesor" — otherwise the
            // relatedness judge would be comparing a misspelling that
            // means nothing to it against a real word.
            String wordText = wordSpellingCorrector.correct(command.wordText(), language);
            String previousWord = round.latestChainWord();
            String targetWord = round.targetWordFor(command.playerId());
            WordRelation relationToPrevious;
            try {
                relationToPrevious = wordRelationChecker.relatedness(wordText, previousWord, language);
            } catch (WordRelationCheckException e) {
                throw new WordComparisonFailedException();
            }
            int relatednessToPrevious = relationToPrevious.percentage();
            boolean accepted = relatednessToPrevious >= scoringPolicy.relatednessThreshold();

            Integer relatednessToTarget = null;
            boolean reachedTarget = false;
            boolean metTargetBonus = false;
            boolean reachedGroupTarget = false;
            boolean reachedInfiltratorTarget = false;
            if (accepted) {
                boolean matchesOwnTarget = wordText.trim().equalsIgnoreCase(targetWord.trim());
                try {
                    relatednessToTarget = matchesOwnTarget
                            ? 100
                            : wordRelationChecker.relatedness(wordText, targetWord, language).percentage();
                } catch (WordRelationCheckException e) {
                    throw new WordComparisonFailedException();
                }
                metTargetBonus = relatednessToTarget >= scoringPolicy.relatednessThreshold();
                // Whether this word actually completes a mission is a
                // separate question from "does it match your own target":
                // the infiltrator writing their own secret target does not
                // count (see Round.reachesInfiltratorTarget), even though it
                // still earns them the ordinary target-relatedness bonus
                // above. These two outcomes lead to very different endings —
                // one advances the phase, the other ends the whole game as a
                // loss — so they're kept as separate booleans rather than
                // folded into one "reachedTarget" flag.
                reachedGroupTarget = round.reachesGroupTarget(wordText);
                reachedInfiltratorTarget = round.reachesInfiltratorTarget(command.playerId(), wordText);
                reachedTarget = reachedGroupTarget || reachedInfiltratorTarget;
            }

            int scoreDelta = scoringPolicy.scoreWordAttempt(accepted, relatednessToPrevious, metTargetBonus);
            round.submitWord(
                    command.playerId(), wordText,
                    new WordJudgement(
                            accepted, relatednessToPrevious, relationToPrevious.justification(),
                            relatednessToTarget, reachedTarget));
            r.awardPoints(command.playerId(), scoreDelta);

            if (reachedInfiltratorTarget) {
                r.loseToInfiltrator(Instant.now());
                phaseScheduler.cancel(r.code());
            } else if (reachedGroupTarget) {
                endWordChain(r, round);
            } else {
                endWordChainOrScheduleNextTurn(r);
            }
            return r;
        });
        roomNotifier.notifyRoomUpdated(room);
    }

    @Override
    public void submitVote(SubmitVoteCommand command) {
        // Accusations never end the vote early on "everyone voted" — only
        // the VOTING phase timer (see forceAdvance) reveals the round, so a
        // vote here just records/replaces the player's current pick and
        // lets every client see it update live.
        Room room = roomRepository.mutate(command.roomCode(), r -> {
            requireValidToken(r, command.playerId(), command.reconnectToken());
            Round round = r.round();
            round.submitVote(new Vote(command.playerId(), command.suspectPlayerId()));
            return r;
        });
        roomNotifier.notifyRoomUpdated(room);
    }

    /**
     * Spends the caller's one-per-game rewind power (see
     * Round.rewindLastAcceptedWord): the most recently accepted word in the
     * chain — regardless of who wrote it — is dropped from the log, and
     * whatever score its author earned for that one attempt is handed back
     * via awardPoints with a negated delta. Deliberately does not touch
     * currentTurnIndex or re-schedule the turn timeout: the caller must
     * already be the current-turn player (Round enforces this), and
     * rewinding doesn't end their turn or hand it to anyone else, so the
     * timeout already armed for this turn stays valid.
     */
    @Override
    public void rewindWord(RewindWordCommand command) {
        Room room = roomRepository.mutate(command.roomCode(), r -> {
            requireValidToken(r, command.playerId(), command.reconnectToken());
            Round round = r.round();
            ChainAttempt undone = round.rewindLastAcceptedWord(command.playerId());
            boolean metTargetBonus = undone.relatednessToTarget() != null
                    && undone.relatednessToTarget() >= scoringPolicy.relatednessThreshold();
            int scoreDelta = scoringPolicy.scoreWordAttempt(true, undone.relatednessToPrevious(), metTargetBonus);
            r.awardPoints(undone.authorPlayerId(), -scoreDelta);
            return r;
        });
        roomNotifier.notifyRoomUpdated(room);
    }

    @Override
    public void updateRoomSettings(UpdateRoomSettingsCommand command) {
        Room room = roomRepository.mutate(command.roomCode(), r -> {
            requireValidToken(r, command.playerId(), command.reconnectToken());
            r.requireHost(command.playerId());
            RoomSettings newSettings = new RoomSettings(
                    command.wordTimeSeconds(), command.voteTimeSeconds(), command.language(),
                    command.infiltratorCount(), command.phaseCount(), false);
            r.updateSettings(newSettings, Instant.now());
            return r;
        });
        roomNotifier.notifyRoomUpdated(room);
    }

    @Override
    public void kickPlayer(KickPlayerCommand command) {
        Room room = roomRepository.mutate(command.roomCode(), r -> {
            requireValidToken(r, command.playerId(), command.reconnectToken());
            r.requireHost(command.playerId());
            r.kickPlayer(command.targetPlayerId(), Instant.now());
            // Mirrors handleDisconnect: don't make everyone wait out the
            // kicked player's own turn timeout if it was theirs when they
            // got removed — cancel it and immediately re-derive whose turn
            // it is (or end the chain), same as a dropped connection would.
            if (r.status() == RoomStatus.IN_PROGRESS && r.hasRound()
                    && r.round().phase() == RoundPhase.WORD_CHAIN
                    && r.round().currentTurnPlayerId().equals(command.targetPlayerId())) {
                phaseScheduler.cancel(r.code());
                endWordChainOrScheduleNextTurn(r);
            }
            return r;
        });
        roomNotifier.notifyRoomUpdated(room);
        roomNotifier.notifyPlayerKicked(command.roomCode(), command.targetPlayerId());
    }

    @Override
    public void resetRoom(ResetRoomCommand command) {
        Room room = roomRepository.mutate(command.roomCode(), r -> {
            requireValidToken(r, command.playerId(), command.reconnectToken());
            r.requireHost(command.playerId());
            r.resetToLobby(Instant.now());
            return r;
        });
        roomNotifier.notifyRoomUpdated(room);
    }

    @Override
    public void forceAdvance(AdvancePhaseCommand command) {
        Room room = roomRepository.mutate(command.roomCode(), r -> {
            if (r.status() != RoomStatus.IN_PROGRESS) {
                return null;
            }
            Round round = r.round();
            boolean stale = round.phaseIndex() != command.expectedPhaseIndex()
                    || round.turnsPlayed() != command.expectedTurnsPlayed()
                    || round.phase() != command.expectedPhase();
            if (stale) {
                return null;
            }

            if (command.expectedPhase() == RoundPhase.WORD_CHAIN) {
                round.skipCurrentTurn(round.currentTurnPlayerId());
                endWordChainOrScheduleNextTurn(r);
                return r;
            } else if (command.expectedPhase() == RoundPhase.VOTING) {
                r.revealRound(scoringPolicy, Instant.now());
                return r;
            }
            return null;
        });
        if (room != null) {
            roomNotifier.notifyRoomUpdated(room);
        }
    }

    /**
     * The shared "what happens after a word turn ends" logic: skips over
     * any current-turn player who is already disconnected, bounded by one
     * full lap of the turn order so this can never loop forever even if
     * nobody is left connected — if a full lap still lands on a
     * disconnected player, the timeout is armed anyway and will simply
     * skip them again when it fires. There is no turn/word cap on
     * WORD_CHAIN itself: it only ends once someone reaches the target (see
     * submitWord's reachedTarget branch), so a chain can run indefinitely.
     */
    private void endWordChainOrScheduleNextTurn(Room room) {
        Round round = room.round();
        int playersToCheck = round.turnOrder().size();
        for (int i = 0; i < playersToCheck && isCurrentTurnDisconnected(room, round); i++) {
            round.skipCurrentTurn(round.currentTurnPlayerId());
        }
        scheduleWordTurnTimeout(room);
    }

    private boolean isCurrentTurnDisconnected(Room room, Round round) {
        return room.findPlayer(round.currentTurnPlayerId()).map(player -> !player.isConnected()).orElse(false);
    }

    /**
     * Where WORD_CHAIN ends up depends on whether more chained phases
     * remain: if so, the next phase begins (its words were already dealt
     * up front — see ChainWordBank.fullChain). Otherwise it depends on
     * whether this room has any infiltrators at all: a room too small to
     * field one (see Room.start/maxInfiltratorCount) finishes
     * cooperatively with no accusation to make, everyone else goes on to
     * VOTING as usual - the only trigger that reaches here is reaching the
     * phase's group target word (see submitWord's reachedGroupTarget
     * branch; reaching the infiltrator's target instead ends the game
     * immediately as a loss and never reaches this method).
     */
    private void endWordChain(Room room, Round round) {
        if (round.hasMorePhases()) {
            round.advancePhase();
            scheduleWordTurnTimeout(room);
        } else if (round.infiltratorPlayerIds().isEmpty()) {
            room.finishCooperatively(Instant.now());
            phaseScheduler.cancel(room.code());
        } else {
            advanceFromWordChainToVoting(room, round);
        }
    }

    /**
     * A daily challenge has no per-turn time limit: it never arms a
     * PhaseScheduler timeout and leaves phaseDeadline null, so the
     * frontend's countdown (driven entirely by phaseDeadline) simply
     * renders nothing rather than a "0 players left to skip" race. Daily
     * rooms always have 0 infiltrators (see RoomSettings.daily), so
     * advanceFromWordChainToVoting is never reached for them anyway — this
     * guard only needs to cover the timeout armed after every submitted
     * word.
     */
    private void scheduleWordTurnTimeout(Room room) {
        if (room.settings().daily()) {
            return;
        }
        Round round = room.round();
        Instant deadline = Instant.now().plusSeconds(room.settings().wordTimeSeconds());
        round.setPhaseDeadline(deadline);
        phaseScheduler.scheduleAdvance(
                room.code(), round.phaseIndex(), round.turnsPlayed(), RoundPhase.WORD_CHAIN, deadline);
    }

    private void advanceFromWordChainToVoting(Room room, Round round) {
        Instant deadline = Instant.now().plusSeconds(room.settings().voteTimeSeconds());
        round.startVoting(deadline);
        phaseScheduler.scheduleAdvance(
                room.code(), round.phaseIndex(), round.turnsPlayed(), RoundPhase.VOTING, deadline);
    }

    private void requireValidToken(Room room, String playerId, String reconnectToken) {
        Player player = room.findPlayer(playerId).orElseThrow(() -> new PlayerNotFoundException(playerId));
        if (!player.matchesReconnectToken(reconnectToken)) {
            throw new PlayerNotFoundException(playerId);
        }
        // A kicked player is kept in the roster (see Room.kickPlayer) so
        // Round's already-dealt turnOrder can still find and auto-skip
        // them, but every action of theirs — including reconnect — must
        // keep failing exactly as if they were never in the room at all.
        if (player.isKicked()) {
            throw new PlayerNotFoundException(playerId);
        }
    }

    private String generateUniqueRoomCode() {
        String code;
        do {
            code = roomCodeGenerator.generate();
        } while (roomRepository.existsByCode(code));
        return code;
    }
}
