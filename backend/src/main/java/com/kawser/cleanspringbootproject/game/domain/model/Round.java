package com.kawser.cleanspringbootproject.game.domain.model;

import com.kawser.cleanspringbootproject.game.domain.exception.AnswerAlreadySubmittedException;
import com.kawser.cleanspringbootproject.game.domain.exception.AnswerNotFoundException;
import com.kawser.cleanspringbootproject.game.domain.exception.AnswerNotSubmittedException;
import com.kawser.cleanspringbootproject.game.domain.exception.SelfVoteNotAllowedException;
import com.kawser.cleanspringbootproject.game.domain.exception.VoteAlreadySubmittedException;
import com.kawser.cleanspringbootproject.game.domain.exception.WrongPhaseException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A single round of play: one question, every submitted answer (including
 * the AI's), and the votes cast against those answers. Owns the
 * SHOWING_QUESTION -> ANSWERING -> VOTING -> REVEAL phase transitions and
 * the invariants tied to each phase.
 */
public class Round {

    private final int roundNumber;
    private final Question question;
    private final List<Answer> answers = new ArrayList<>();
    private final List<Vote> votes = new ArrayList<>();
    private RoundPhase phase;
    private Instant phaseDeadline;

    public Round(int roundNumber, Question question) {
        this.roundNumber = roundNumber;
        this.question = question;
        this.phase = RoundPhase.ANSWERING;
    }

    public void submitAnswer(Answer answer) {
        requirePhase(RoundPhase.ANSWERING);
        if (hasAnswerFrom(answer.authorPlayerId())) {
            throw new AnswerAlreadySubmittedException(answer.authorPlayerId());
        }
        answers.add(answer);
    }

    /**
     * Lets a player take back their submitted answer while ANSWERING is
     * still active, so they can edit and resubmit before the phase moves
     * on. Only valid up to that point: once VOTING starts (whether by
     * everyone answering or the timeout), there is nothing left to
     * retract.
     */
    public void retractAnswer(String playerId) {
        requirePhase(RoundPhase.ANSWERING);
        boolean removed = answers.removeIf(answer -> !answer.isAi() && playerId.equals(answer.authorPlayerId()));
        if (!removed) {
            throw new AnswerNotSubmittedException(playerId);
        }
    }

    public void submitVote(Vote vote) {
        requirePhase(RoundPhase.VOTING);
        if (hasVoteFrom(vote.voterPlayerId())) {
            throw new VoteAlreadySubmittedException(vote.voterPlayerId());
        }
        Answer votedAnswer = findAnswer(vote.answerId())
                .orElseThrow(() -> new AnswerNotFoundException(vote.answerId()));
        if (vote.voterPlayerId().equals(votedAnswer.authorPlayerId())) {
            throw new SelfVoteNotAllowedException(vote.voterPlayerId());
        }
        votes.add(vote);
    }

    public boolean allPlayersAnswered(int expectedHumanPlayerCount) {
        long humanAnswers = answers.stream().filter(answer -> !answer.isAi()).count();
        return humanAnswers >= expectedHumanPlayerCount;
    }

    public boolean allPlayersVoted(int expectedVoterCount) {
        return votes.size() >= expectedVoterCount;
    }

    public void startVoting(Instant deadline) {
        requirePhase(RoundPhase.ANSWERING);
        this.phase = RoundPhase.VOTING;
        this.phaseDeadline = deadline;
    }

    public void reveal() {
        requirePhase(RoundPhase.VOTING);
        this.phase = RoundPhase.REVEAL;
        this.phaseDeadline = null;
    }

    private void requirePhase(RoundPhase expected) {
        if (phase != expected) {
            throw new WrongPhaseException(expected, phase);
        }
    }

    private boolean hasAnswerFrom(String playerId) {
        return answers.stream().anyMatch(answer -> playerId.equals(answer.authorPlayerId()));
    }

    private boolean hasVoteFrom(String playerId) {
        return votes.stream().anyMatch(vote -> playerId.equals(vote.voterPlayerId()));
    }

    private Optional<Answer> findAnswer(String answerId) {
        return answers.stream().filter(answer -> answer.id().equals(answerId)).findFirst();
    }

    public Optional<Answer> aiAnswer() {
        return answers.stream().filter(Answer::isAi).findFirst();
    }

    public int roundNumber() {
        return roundNumber;
    }

    public Question question() {
        return question;
    }

    public RoundPhase phase() {
        return phase;
    }

    public List<Answer> answers() {
        return List.copyOf(answers);
    }

    public List<Vote> votes() {
        return List.copyOf(votes);
    }

    public Instant phaseDeadline() {
        return phaseDeadline;
    }

    public void setPhaseDeadline(Instant phaseDeadline) {
        this.phaseDeadline = phaseDeadline;
    }
}
