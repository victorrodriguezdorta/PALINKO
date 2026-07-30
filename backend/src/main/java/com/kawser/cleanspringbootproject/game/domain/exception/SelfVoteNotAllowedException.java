package com.kawser.cleanspringbootproject.game.domain.exception;

import java.util.Map;

public class SelfVoteNotAllowedException extends GameDomainException {

    public SelfVoteNotAllowedException(String playerId) {
        super("SELF_VOTE_NOT_ALLOWED", "Player " + playerId + " cannot vote for their own answer",
                Map.of("playerId", playerId));
    }
}
