package com.kawser.cleanspringbootproject.game.application.service;

import com.kawser.cleanspringbootproject.game.application.dto.AdvancePhaseCommand;
import com.kawser.cleanspringbootproject.game.application.dto.CreateDailyRoomCommand;
import com.kawser.cleanspringbootproject.game.application.dto.CreateRoomCommand;
import com.kawser.cleanspringbootproject.game.application.dto.CreateRoomResult;
import com.kawser.cleanspringbootproject.game.application.dto.DisconnectCommand;
import com.kawser.cleanspringbootproject.game.application.dto.JoinRoomCommand;
import com.kawser.cleanspringbootproject.game.application.dto.JoinRoomResult;
import com.kawser.cleanspringbootproject.game.application.dto.ReconnectCommand;
import com.kawser.cleanspringbootproject.game.application.dto.ResetRoomCommand;
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
import com.kawser.cleanspringbootproject.game.application.port.in.ReconnectPlayerUseCase;
import com.kawser.cleanspringbootproject.game.application.port.in.ResetRoomUseCase;
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
import com.kawser.cleanspringbootproject.game.application.port.out.WordRelationChecker;
import com.kawser.cleanspringbootproject.game.domain.exception.PlayerNotFoundException;
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
        ReconnectPlayerUseCase,
        HandleDisconnectUseCase,
        StartGameUseCase,
        SubmitWordUseCase,
        SubmitVoteUseCase,
        AdvancePhaseUseCase,
        ResetRoomUseCase,
        UpdateRoomSettingsUseCase {

    private static final int DAILY_PHASE_COUNT = 3;
    private static final String DAILY_PLAYER_NAME = "#";

    private final RoomRepository roomRepository;
    private final RoomNotifier roomNotifier;
    private final WordRelationChecker wordRelationChecker;
    private final ChainWordBank chainWordBank;
    private final RoomCodeGenerator roomCodeGenerator;
    private final PhaseScheduler phaseScheduler;
    private final ScoringPolicy scoringPolicy;

    public GameApplicationService(
            RoomRepository roomRepository,
            RoomNotifier roomNotifier,
            WordRelationChecker wordRelationChecker,
            ChainWordBank chainWordBank,
            RoomCodeGenerator roomCodeGenerator,
            PhaseScheduler phaseScheduler,
            ScoringPolicy scoringPolicy) {
        this.roomRepository = roomRepository;
        this.roomNotifier = roomNotifier;
        this.wordRelationChecker = wordRelationChecker;
        this.chainWordBank = chainWordBank;
        this.roomCodeGenerator = roomCodeGenerator;
        this.phaseScheduler = phaseScheduler;
        this.scoringPolicy = scoringPolicy;
    }

    @Override
    public CreateRoomResult createRoom(CreateRoomCommand command) {
        String code = generateUniqueRoomCode();
        String hostId = UUID.randomUUID().toString();
        String hostToken = UUID.randomUUID().toString();
        Player host = Player.host(hostId, hostToken, command.hostName());

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
        // A solo daily room has nobody else to show a name to, so the
        // player never types one — every daily room's sole player is named
        // "#" rather than taking arbitrary client input here.
        Player host = Player.host(hostId, hostToken, DAILY_PLAYER_NAME);

        long seed = DailySeed.seedFor(DailySeed.today(), command.language());
        List<WordSet> phaseWordSets =
                chainWordBank.fullChain(command.language(), DAILY_PHASE_COUNT, seed);

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
        Player guest = Player.guest(playerId, reconnectToken, command.playerName());

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
        Room room = roomRepository.mutate(command.roomCode(), r -> {
            if (r.isHost(command.playerId())) {
                // The host leaving ends the game for everyone rather than
                // just marking one more player disconnected: there is no
                // one left who could start the game or otherwise keep it
                // moving.
                r.close(Instant.now());
                phaseScheduler.cancel(r.code());
            } else {
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
            }
            return r;
        });
        roomNotifier.notifyRoomUpdated(room);
        if (room.isHost(command.playerId())) {
            // Deleted only after the CLOSED snapshot above has already been
            // built from this in-memory Room and pushed to whoever is still
            // connected — removing it from the repository here doesn't
            // affect that already-sent payload, it just stops the room from
            // being findable (by reconnect, join, or the cleanup sweep)
            // ever again, instead of lingering until the sweep's TTL.
            roomRepository.deleteByCode(room.code());
        }
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
        Room room = roomRepository.mutate(command.roomCode(), r -> {
            requireValidToken(r, command.playerId(), command.reconnectToken());
            Round round = r.round();
            round.requireCurrentTurn(command.playerId());

            GameLanguage language = r.settings().language();
            String previousWord = round.latestChainWord();
            String targetWord = round.targetWordFor(command.playerId());
            WordRelation relationToPrevious = wordRelationChecker.relatedness(command.wordText(), previousWord, language);
            int relatednessToPrevious = relationToPrevious.percentage();
            boolean accepted = relatednessToPrevious >= scoringPolicy.relatednessThreshold();

            Integer relatednessToTarget = null;
            boolean reachedTarget = false;
            boolean metTargetBonus = false;
            boolean reachedGroupTarget = false;
            boolean reachedInfiltratorTarget = false;
            if (accepted) {
                boolean matchesOwnTarget = command.wordText().trim().equalsIgnoreCase(targetWord.trim());
                relatednessToTarget = matchesOwnTarget
                        ? 100
                        : wordRelationChecker.relatedness(command.wordText(), targetWord, language).percentage();
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
                reachedGroupTarget = round.reachesGroupTarget(command.wordText());
                reachedInfiltratorTarget = round.reachesInfiltratorTarget(command.playerId(), command.wordText());
                reachedTarget = reachedGroupTarget || reachedInfiltratorTarget;
            }

            int scoreDelta = scoringPolicy.scoreWordAttempt(accepted, relatednessToPrevious, metTargetBonus);
            round.submitWord(
                    command.playerId(), command.wordText(),
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
    }

    private String generateUniqueRoomCode() {
        String code;
        do {
            code = roomCodeGenerator.generate();
        } while (roomRepository.existsByCode(code));
        return code;
    }
}
