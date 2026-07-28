package com.kawser.cleanspringbootproject.game.application.service;

import com.kawser.cleanspringbootproject.game.application.dto.AdvancePhaseCommand;
import com.kawser.cleanspringbootproject.game.application.dto.AdvanceToNextRoundCommand;
import com.kawser.cleanspringbootproject.game.application.dto.CreateRoomCommand;
import com.kawser.cleanspringbootproject.game.application.dto.CreateRoomResult;
import com.kawser.cleanspringbootproject.game.application.dto.DisconnectCommand;
import com.kawser.cleanspringbootproject.game.application.dto.JoinRoomCommand;
import com.kawser.cleanspringbootproject.game.application.dto.JoinRoomResult;
import com.kawser.cleanspringbootproject.game.application.dto.ReconnectCommand;
import com.kawser.cleanspringbootproject.game.application.dto.ResetRoomCommand;
import com.kawser.cleanspringbootproject.game.application.dto.RetractAnswerCommand;
import com.kawser.cleanspringbootproject.game.application.dto.RoomSnapshot;
import com.kawser.cleanspringbootproject.game.application.dto.StartGameCommand;
import com.kawser.cleanspringbootproject.game.application.dto.SubmitAnswerCommand;
import com.kawser.cleanspringbootproject.game.application.dto.SubmitVoteCommand;
import com.kawser.cleanspringbootproject.game.application.dto.UpdateRoomSettingsCommand;
import com.kawser.cleanspringbootproject.game.application.port.in.AdvancePhaseUseCase;
import com.kawser.cleanspringbootproject.game.application.port.in.AdvanceToNextRoundUseCase;
import com.kawser.cleanspringbootproject.game.application.port.in.CreateRoomUseCase;
import com.kawser.cleanspringbootproject.game.application.port.in.HandleDisconnectUseCase;
import com.kawser.cleanspringbootproject.game.application.port.in.JoinRoomUseCase;
import com.kawser.cleanspringbootproject.game.application.port.in.ReconnectPlayerUseCase;
import com.kawser.cleanspringbootproject.game.application.port.in.ResetRoomUseCase;
import com.kawser.cleanspringbootproject.game.application.port.in.RetractAnswerUseCase;
import com.kawser.cleanspringbootproject.game.application.port.in.StartGameUseCase;
import com.kawser.cleanspringbootproject.game.application.port.in.SubmitAnswerUseCase;
import com.kawser.cleanspringbootproject.game.application.port.in.SubmitVoteUseCase;
import com.kawser.cleanspringbootproject.game.application.port.in.UpdateRoomSettingsUseCase;
import com.kawser.cleanspringbootproject.game.application.port.out.AiAnswerGenerator;
import com.kawser.cleanspringbootproject.game.application.port.out.PhaseScheduler;
import com.kawser.cleanspringbootproject.game.application.port.out.QuestionBank;
import com.kawser.cleanspringbootproject.game.application.port.out.RoomCodeGenerator;
import com.kawser.cleanspringbootproject.game.application.port.out.RoomNotifier;
import com.kawser.cleanspringbootproject.game.application.port.out.RoomRepository;
import com.kawser.cleanspringbootproject.game.domain.exception.PlayerNotFoundException;
import com.kawser.cleanspringbootproject.game.domain.exception.RoomNotFoundException;
import com.kawser.cleanspringbootproject.game.domain.model.Answer;
import com.kawser.cleanspringbootproject.game.domain.model.Player;
import com.kawser.cleanspringbootproject.game.domain.model.Question;
import com.kawser.cleanspringbootproject.game.domain.model.Room;
import com.kawser.cleanspringbootproject.game.domain.model.RoomSettings;
import com.kawser.cleanspringbootproject.game.domain.model.Round;
import com.kawser.cleanspringbootproject.game.domain.model.RoundPhase;
import com.kawser.cleanspringbootproject.game.domain.model.RoundResult;
import com.kawser.cleanspringbootproject.game.domain.model.Vote;
import com.kawser.cleanspringbootproject.game.domain.service.ScoringPolicy;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Orchestrates every use case for the game module. Kept as a single class
 * (rather than one implementation class per port.in interface) because all
 * of them share the same four collaborators and the same per-room locking
 * discipline — splitting them would multiply boilerplate without adding any
 * real substitutability.
 */
public class GameApplicationService implements
        CreateRoomUseCase,
        JoinRoomUseCase,
        ReconnectPlayerUseCase,
        HandleDisconnectUseCase,
        StartGameUseCase,
        SubmitAnswerUseCase,
        SubmitVoteUseCase,
        AdvanceToNextRoundUseCase,
        AdvancePhaseUseCase,
        ResetRoomUseCase,
        UpdateRoomSettingsUseCase,
        RetractAnswerUseCase {

    private final RoomRepository roomRepository;
    private final RoomNotifier roomNotifier;
    private final AiAnswerGenerator aiAnswerGenerator;
    private final QuestionBank questionBank;
    private final RoomCodeGenerator roomCodeGenerator;
    private final PhaseScheduler phaseScheduler;
    private final ScoringPolicy scoringPolicy;

    public GameApplicationService(
            RoomRepository roomRepository,
            RoomNotifier roomNotifier,
            AiAnswerGenerator aiAnswerGenerator,
            QuestionBank questionBank,
            RoomCodeGenerator roomCodeGenerator,
            PhaseScheduler phaseScheduler,
            ScoringPolicy scoringPolicy) {
        this.roomRepository = roomRepository;
        this.roomNotifier = roomNotifier;
        this.aiAnswerGenerator = aiAnswerGenerator;
        this.questionBank = questionBank;
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
        var settings = new com.kawser.cleanspringbootproject.game.domain.model.RoomSettings(
                command.totalRounds(), command.answerTimeSeconds(), command.voteTimeSeconds());
        Room room = Room.create(code, settings, host, now);
        roomRepository.save(room);

        RoomSnapshot snapshot = RoomSnapshot.from(room);
        return new CreateRoomResult(code, hostId, hostToken, snapshot);
    }

    @Override
    public JoinRoomResult joinRoom(JoinRoomCommand command) {
        String playerId = UUID.randomUUID().toString();
        String reconnectToken = UUID.randomUUID().toString();
        Player guest = Player.guest(playerId, reconnectToken, command.playerName());

        RoomSnapshot snapshot = roomRepository.mutate(command.roomCode(), room -> {
            room.addPlayer(guest, Instant.now());
            return RoomSnapshot.from(room);
        });

        roomNotifier.notifyRoomUpdated(snapshot);
        return new JoinRoomResult(playerId, reconnectToken, snapshot);
    }

    @Override
    public RoomSnapshot reconnect(ReconnectCommand command) {
        RoomSnapshot snapshot = roomRepository.mutate(command.roomCode(), room -> {
            requireValidToken(room, command.playerId(), command.reconnectToken());
            room.markReconnected(command.playerId(), Instant.now());
            return RoomSnapshot.from(room);
        });
        roomNotifier.notifyRoomUpdated(snapshot);
        return snapshot;
    }

    @Override
    public void handleDisconnect(DisconnectCommand command) {
        RoomSnapshot snapshot = roomRepository.mutate(command.roomCode(), room -> {
            if (room.isHost(command.playerId())) {
                // The host leaving ends the game for everyone rather than
                // just marking one more player disconnected: there is no
                // one left who could start the game, advance a round, or
                // otherwise keep it moving.
                room.close(Instant.now());
                phaseScheduler.cancel(room.code());
            } else {
                room.markDisconnected(command.playerId(), Instant.now());
            }
            return RoomSnapshot.from(room);
        });
        roomNotifier.notifyRoomUpdated(snapshot);
    }

    @Override
    public void startGame(StartGameCommand command) {
        RoomSnapshot snapshot = roomRepository.mutate(command.roomCode(), room -> {
            requireValidToken(room, command.playerId(), command.reconnectToken());
            room.requireHost(command.playerId());

            Question firstQuestion = questionBank.nextQuestion(Set.of());
            room.start(firstQuestion, Instant.now());
            addAiAnswerAndScheduleTimeout(room);
            return RoomSnapshot.from(room);
        });
        roomNotifier.notifyRoomUpdated(snapshot);
    }

    @Override
    public void submitAnswer(SubmitAnswerCommand command) {
        RoomSnapshot snapshot = roomRepository.mutate(command.roomCode(), room -> {
            requireValidToken(room, command.playerId(), command.reconnectToken());
            Round round = room.currentRound();
            round.submitAnswer(Answer.human(UUID.randomUUID().toString(), command.playerId(), command.answerText()));

            if (round.allPlayersAnswered(room.connectedHumanPlayerCount())) {
                advanceFromAnsweringToVoting(room, round);
            }
            return RoomSnapshot.from(room);
        });
        roomNotifier.notifyRoomUpdated(snapshot);
    }

    @Override
    public void retractAnswer(RetractAnswerCommand command) {
        RoomSnapshot snapshot = roomRepository.mutate(command.roomCode(), room -> {
            requireValidToken(room, command.playerId(), command.reconnectToken());
            Round round = room.currentRound();
            round.retractAnswer(command.playerId());
            return RoomSnapshot.from(room);
        });
        roomNotifier.notifyRoomUpdated(snapshot);
    }

    @Override
    public void submitVote(SubmitVoteCommand command) {
        RoomSnapshot[] resultHolder = new RoomSnapshot[1];
        roomRepository.mutate(command.roomCode(), room -> {
            requireValidToken(room, command.playerId(), command.reconnectToken());
            Round round = room.currentRound();
            round.submitVote(new Vote(command.playerId(), command.votedAnswerId()));

            if (round.allPlayersVoted(room.connectedHumanPlayerCount())) {
                RoundResult result = revealRound(room, round);
                resultHolder[0] = RoomSnapshot.fromWithResult(room, result);
            } else {
                resultHolder[0] = RoomSnapshot.from(room);
            }
            return null;
        });
        roomNotifier.notifyRoomUpdated(resultHolder[0]);
    }

    @Override
    public void advanceToNextRound(AdvanceToNextRoundCommand command) {
        RoomSnapshot snapshot = roomRepository.mutate(command.roomCode(), room -> {
            requireValidToken(room, command.playerId(), command.reconnectToken());
            room.requireHost(command.playerId());

            Set<String> usedQuestionIds = collectUsedQuestionIds(room);
            Question nextQuestion = room.hasMoreRounds()
                    ? questionBank.nextQuestion(usedQuestionIds)
                    : null;
            room.advanceToNextRoundOrFinish(nextQuestion, Instant.now());

            if (room.status() == com.kawser.cleanspringbootproject.game.domain.model.RoomStatus.IN_PROGRESS) {
                addAiAnswerAndScheduleTimeout(room);
            }
            return RoomSnapshot.from(room);
        });
        roomNotifier.notifyRoomUpdated(snapshot);
    }

    @Override
    public void updateRoomSettings(UpdateRoomSettingsCommand command) {
        RoomSnapshot snapshot = roomRepository.mutate(command.roomCode(), room -> {
            requireValidToken(room, command.playerId(), command.reconnectToken());
            room.requireHost(command.playerId());
            RoomSettings newSettings = new RoomSettings(
                    command.totalRounds(), command.answerTimeSeconds(), command.voteTimeSeconds());
            room.updateSettings(newSettings, Instant.now());
            return RoomSnapshot.from(room);
        });
        roomNotifier.notifyRoomUpdated(snapshot);
    }

    @Override
    public void resetRoom(ResetRoomCommand command) {
        RoomSnapshot snapshot = roomRepository.mutate(command.roomCode(), room -> {
            requireValidToken(room, command.playerId(), command.reconnectToken());
            room.requireHost(command.playerId());
            room.resetToLobby(Instant.now());
            return RoomSnapshot.from(room);
        });
        roomNotifier.notifyRoomUpdated(snapshot);
    }

    @Override
    public void forceAdvance(AdvancePhaseCommand command) {
        RoomSnapshot snapshot = roomRepository.mutate(command.roomCode(), room -> {
            if (room.status() != com.kawser.cleanspringbootproject.game.domain.model.RoomStatus.IN_PROGRESS) {
                return null;
            }
            Round round = room.currentRound();
            boolean stale = round.roundNumber() != command.roundNumber() || round.phase() != command.expectedPhase();
            if (stale) {
                return null;
            }

            if (command.expectedPhase() == RoundPhase.ANSWERING) {
                advanceFromAnsweringToVoting(room, round);
                return RoomSnapshot.from(room);
            } else if (command.expectedPhase() == RoundPhase.VOTING) {
                RoundResult result = revealRound(room, round);
                return RoomSnapshot.fromWithResult(room, result);
            }
            return null;
        });
        if (snapshot != null) {
            roomNotifier.notifyRoomUpdated(snapshot);
        }
    }

    private void addAiAnswerAndScheduleTimeout(Room room) {
        Round round = room.currentRound();
        String aiAnswerText = aiAnswerGenerator.generateAnswer(round.question());
        round.submitAnswer(Answer.ai(UUID.randomUUID().toString(), aiAnswerText));

        Instant deadline = Instant.now().plusSeconds(room.settings().answerTimeSeconds());
        round.setPhaseDeadline(deadline);
        phaseScheduler.scheduleAdvance(room.code(), round.roundNumber(), RoundPhase.ANSWERING, deadline);
    }

    private void advanceFromAnsweringToVoting(Room room, Round round) {
        Instant deadline = Instant.now().plusSeconds(room.settings().voteTimeSeconds());
        round.startVoting(deadline);
        phaseScheduler.scheduleAdvance(room.code(), round.roundNumber(), RoundPhase.VOTING, deadline);
    }

    private RoundResult revealRound(Room room, Round round) {
        return room.revealCurrentRound(scoringPolicy, Instant.now());
    }

    private Set<String> collectUsedQuestionIds(Room room) {
        // Only the current game's rounds are tracked in memory, so this is
        // bounded by totalRounds and cheap to recompute each time.
        Set<String> ids = new HashSet<>();
        ids.add(room.currentRound().question().id());
        return ids;
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
