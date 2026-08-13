package com.kawser.cleanspringbootproject.secretmode.application.service;

import com.kawser.cleanspringbootproject.game.application.port.out.WordSpellingCorrector;
import com.kawser.cleanspringbootproject.game.application.port.out.WordSubmissionRateLimiter;
import com.kawser.cleanspringbootproject.game.domain.model.GameLanguage;
import com.kawser.cleanspringbootproject.secretmode.application.dto.CreateSecretDailyRoomCommand;
import com.kawser.cleanspringbootproject.secretmode.application.dto.CreateSecretRoomResult;
import com.kawser.cleanspringbootproject.secretmode.application.dto.DisconnectSecretPlayerCommand;
import com.kawser.cleanspringbootproject.secretmode.application.dto.GiveUpCommand;
import com.kawser.cleanspringbootproject.secretmode.application.dto.ReconnectSecretPlayerCommand;
import com.kawser.cleanspringbootproject.secretmode.application.dto.SecretRoomSnapshot;
import com.kawser.cleanspringbootproject.secretmode.application.dto.SubmitSecretWordCommand;
import com.kawser.cleanspringbootproject.secretmode.application.dto.SubmitSolutionCommand;
import com.kawser.cleanspringbootproject.secretmode.application.dto.UseHintCommand;
import com.kawser.cleanspringbootproject.secretmode.application.port.in.CreateSecretDailyRoomUseCase;
import com.kawser.cleanspringbootproject.secretmode.application.port.in.GiveUpUseCase;
import com.kawser.cleanspringbootproject.secretmode.application.port.in.HandleSecretDisconnectUseCase;
import com.kawser.cleanspringbootproject.secretmode.application.port.in.ReconnectSecretPlayerUseCase;
import com.kawser.cleanspringbootproject.secretmode.application.port.in.SubmitSecretWordUseCase;
import com.kawser.cleanspringbootproject.secretmode.application.port.in.SubmitSolutionUseCase;
import com.kawser.cleanspringbootproject.secretmode.application.port.in.UseHintUseCase;
import com.kawser.cleanspringbootproject.secretmode.application.port.out.ConceptBank;
import com.kawser.cleanspringbootproject.secretmode.application.port.out.SecretQualityCheckException;
import com.kawser.cleanspringbootproject.secretmode.application.port.out.SecretQualityChecker;
import com.kawser.cleanspringbootproject.secretmode.application.port.out.SecretQualityChecker.QualityMatchResult;
import com.kawser.cleanspringbootproject.secretmode.application.port.out.SecretQualityChecker.WordJudgementResult;
import com.kawser.cleanspringbootproject.secretmode.application.port.out.SecretRoomCodeGenerator;
import com.kawser.cleanspringbootproject.secretmode.application.port.out.SecretRoomNotifier;
import com.kawser.cleanspringbootproject.secretmode.application.port.out.SecretRoomRepository;
import com.kawser.cleanspringbootproject.secretmode.domain.exception.ConceptNotAvailableException;
import com.kawser.cleanspringbootproject.secretmode.domain.exception.HintNotAvailableException;
import com.kawser.cleanspringbootproject.secretmode.domain.exception.InvalidSecretWordCharactersException;
import com.kawser.cleanspringbootproject.secretmode.domain.exception.InvalidSolutionTextException;
import com.kawser.cleanspringbootproject.secretmode.domain.exception.NoUnusedAcceptedWordsForHintException;
import com.kawser.cleanspringbootproject.secretmode.domain.exception.SecretPlayerNotFoundException;
import com.kawser.cleanspringbootproject.secretmode.domain.exception.SecretQualityCheckFailedException;
import com.kawser.cleanspringbootproject.secretmode.domain.exception.SecretWordSubmissionRateLimitedException;
import com.kawser.cleanspringbootproject.secretmode.domain.model.Concept;
import com.kawser.cleanspringbootproject.secretmode.domain.model.JudgementSource;
import com.kawser.cleanspringbootproject.secretmode.domain.model.SecretPlayer;
import com.kawser.cleanspringbootproject.secretmode.domain.model.SecretRoom;
import com.kawser.cleanspringbootproject.secretmode.domain.service.SecretScoringPolicy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Orchestrates every use case for secretmode. Kept as a single class (rather
 * than one implementation class per port.in interface) for the same reason
 * as game.application.service.GameApplicationService: every use case shares
 * the same collaborators and the same per-room locking discipline.
 */
public class SecretGameApplicationService implements
        CreateSecretDailyRoomUseCase,
        ReconnectSecretPlayerUseCase,
        HandleSecretDisconnectUseCase,
        SubmitSecretWordUseCase,
        UseHintUseCase,
        SubmitSolutionUseCase,
        GiveUpUseCase {

    private static final int MAX_FORMULATION_LENGTH = 280;
    // Letters only, at most one interior space - identical shape check to
    // GameApplicationService.VALID_WORD_PATTERN, reused verbatim so
    // secretmode rejects the same "uva-coche" smuggled-compound shapes
    // before ever spending an AI call.
    private static final Pattern VALID_WORD_PATTERN = Pattern.compile("^\\p{L}+( \\p{L}+)?$");

    private final SecretRoomRepository secretRoomRepository;
    private final SecretRoomNotifier secretRoomNotifier;
    private final SecretQualityChecker secretQualityChecker;
    private final WordSpellingCorrector wordSpellingCorrector;
    private final ConceptBank conceptBank;
    private final SecretRoomCodeGenerator secretRoomCodeGenerator;
    private final SecretScoringPolicy secretScoringPolicy;
    private final WordSubmissionRateLimiter wordSubmissionRateLimiter;
    private final Random random = new Random();

    public SecretGameApplicationService(
            SecretRoomRepository secretRoomRepository,
            SecretRoomNotifier secretRoomNotifier,
            SecretQualityChecker secretQualityChecker,
            WordSpellingCorrector wordSpellingCorrector,
            ConceptBank conceptBank,
            SecretRoomCodeGenerator secretRoomCodeGenerator,
            SecretScoringPolicy secretScoringPolicy,
            WordSubmissionRateLimiter wordSubmissionRateLimiter) {
        this.secretRoomRepository = secretRoomRepository;
        this.secretRoomNotifier = secretRoomNotifier;
        this.secretQualityChecker = secretQualityChecker;
        this.wordSpellingCorrector = wordSpellingCorrector;
        this.conceptBank = conceptBank;
        this.secretRoomCodeGenerator = secretRoomCodeGenerator;
        this.secretScoringPolicy = secretScoringPolicy;
        this.wordSubmissionRateLimiter = wordSubmissionRateLimiter;
    }

    /**
     * A daily secret-mode room is solo and starts already IN_PROGRESS, just
     * like GameApplicationService.createDailyRoom: there is no lobby to sit
     * in, and every player who opens the challenge on the same UTC date
     * gets the same Concept (see ConceptBank.dailyConceptFor).
     */
    @Override
    public CreateSecretRoomResult createSecretDailyRoom(CreateSecretDailyRoomCommand command) {
        Concept concept = conceptBank.dailyConceptFor(SecretDailySeed.today());
        if (concept == null) {
            throw new ConceptNotAvailableException("daily");
        }

        String code = generateUniqueRoomCode();
        String playerId = UUID.randomUUID().toString();
        String reconnectToken = UUID.randomUUID().toString();
        SecretPlayer player = SecretPlayer.solo(playerId, reconnectToken);

        Instant now = Instant.now();
        SecretRoom room = SecretRoom.create(code, concept, player, now);
        secretRoomRepository.save(room);

        return new CreateSecretRoomResult(code, playerId, reconnectToken, SecretRoomSnapshot.from(room));
    }

    @Override
    public SecretRoomSnapshot reconnect(ReconnectSecretPlayerCommand command) {
        SecretRoom room = secretRoomRepository.mutate(command.roomCode(), r -> {
            requireValidToken(r, command.playerId(), command.reconnectToken());
            r.markReconnected(Instant.now());
            return r;
        });
        secretRoomNotifier.notifyRoomUpdated(room);
        return SecretRoomSnapshot.from(room);
    }

    @Override
    public void handleDisconnect(DisconnectSecretPlayerCommand command) {
        SecretRoom room = secretRoomRepository.mutate(command.roomCode(), r -> {
            r.markDisconnected(Instant.now());
            return r;
        });
        secretRoomNotifier.notifyRoomUpdated(room);
    }

    @Override
    public void submitWord(SubmitSecretWordCommand command) {
        if (command.wordText() == null || command.wordText().isBlank()) {
            throw new IllegalArgumentException("wordText must not be blank");
        }
        if (!VALID_WORD_PATTERN.matcher(command.wordText().trim()).matches()) {
            throw new InvalidSecretWordCharactersException(command.wordText());
        }
        if (!wordSubmissionRateLimiter.tryAcquire(command.playerId())) {
            throw new SecretWordSubmissionRateLimitedException();
        }

        SecretRoom room = secretRoomRepository.mutate(command.roomCode(), r -> {
            requireValidToken(r, command.playerId(), command.reconnectToken());
            GameLanguage language = r.concept().language();
            String corrected = wordSpellingCorrector.correct(command.wordText(), language);
            String normalized = corrected.trim().toLowerCase();

            // Already tried before: return the same verdict without a new
            // AI call, without touching totalFailures, without scoring it
            // again - see SecretRoom.recordWordAttempt's dedupe.
            if (r.triedWordsLowercase().contains(normalized)) {
                return r;
            }

            boolean accepted;
            JudgementSource source;
            if (r.concept().acceptedWords().contains(normalized)) {
                accepted = true;
                source = JudgementSource.WORD_BANK;
            } else if (r.concept().rejectedWords().contains(normalized)) {
                accepted = false;
                source = JudgementSource.WORD_BANK;
            } else {
                try {
                    WordJudgementResult result =
                            secretQualityChecker.judgeWord(corrected, r.concept().hiddenQuality(), language);
                    accepted = result.accepted();
                    source = JudgementSource.AI;
                } catch (SecretQualityCheckException e) {
                    throw new SecretQualityCheckFailedException();
                }
            }
            r.recordWordAttempt(corrected, accepted, source, Instant.now());
            return r;
        });
        secretRoomNotifier.notifyRoomUpdated(room);
    }

    /**
     * Reveals a random accepted word from the concept's bank that the
     * player hasn't already tried, spending one of their banked hints (see
     * SecretRoom.hintsAvailable). The pick itself lives here rather than in
     * SecretRoom because it needs Concept.acceptedWords() filtered against
     * SecretRoom.triedWordsLowercase() - a read across both, not a single
     * aggregate's own state.
     */
    @Override
    public void useHint(UseHintCommand command) {
        SecretRoom room = secretRoomRepository.mutate(command.roomCode(), r -> {
            requireValidToken(r, command.playerId(), command.reconnectToken());
            if (r.hintsAvailable() <= 0) {
                throw new HintNotAvailableException(r.code());
            }
            List<String> pool = new ArrayList<>(r.concept().acceptedWords());
            pool.removeAll(r.triedWordsLowercase());
            if (pool.isEmpty()) {
                throw new NoUnusedAcceptedWordsForHintException(r.code());
            }
            String revealed = pool.get(random.nextInt(pool.size()));
            r.useHint(revealed, Instant.now());
            return r;
        });
        secretRoomNotifier.notifyRoomUpdated(room);
    }

    @Override
    public void submitSolution(SubmitSolutionCommand command) {
        String formulation = command.formulationText() == null ? "" : command.formulationText().trim();
        if (formulation.isEmpty() || formulation.length() > MAX_FORMULATION_LENGTH) {
            throw new InvalidSolutionTextException(command.formulationText());
        }
        // Reuses the same per-player rate limiter as word submissions (see
        // WordSubmissionRateLimiter's javadoc: it's keyed by playerId, not
        // tied to Room) with a distinct key namespace, so a player can't
        // burn through the AI budget by spamming formulations either.
        if (!wordSubmissionRateLimiter.tryAcquire("solution:" + command.playerId())) {
            throw new SecretWordSubmissionRateLimitedException();
        }

        SecretRoom room = secretRoomRepository.mutate(command.roomCode(), r -> {
            requireValidToken(r, command.playerId(), command.reconnectToken());
            QualityMatchResult result = secretQualityChecker.judgeFormulation(
                    formulation, r.concept().hiddenQuality(), r.concept().language());
            try {
                r.recordSolutionAttempt(formulation, result.matchLevel(), secretScoringPolicy, Instant.now());
            } catch (SecretQualityCheckException e) {
                throw new SecretQualityCheckFailedException();
            }
            return r;
        });
        secretRoomNotifier.notifyRoomUpdated(room);
    }

    @Override
    public void giveUp(GiveUpCommand command) {
        SecretRoom room = secretRoomRepository.mutate(command.roomCode(), r -> {
            requireValidToken(r, command.playerId(), command.reconnectToken());
            r.giveUp(secretScoringPolicy, Instant.now());
            return r;
        });
        secretRoomNotifier.notifyRoomUpdated(room);
    }

    private void requireValidToken(SecretRoom room, String playerId, String reconnectToken) {
        if (!room.player().id().equals(playerId) || !room.player().matchesReconnectToken(reconnectToken)) {
            throw new SecretPlayerNotFoundException(playerId);
        }
    }

    private String generateUniqueRoomCode() {
        String code;
        do {
            code = secretRoomCodeGenerator.generate();
        } while (secretRoomRepository.existsByCode(code));
        return code;
    }
}
