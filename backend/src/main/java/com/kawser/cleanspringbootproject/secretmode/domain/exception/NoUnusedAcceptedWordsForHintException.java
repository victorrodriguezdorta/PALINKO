package com.kawser.cleanspringbootproject.secretmode.domain.exception;

import java.util.Map;

/**
 * Edge case: the concept's whole accepted-word bank has already been tried
 * by the player, so there is no untried word left to reveal as a hint.
 */
public class NoUnusedAcceptedWordsForHintException extends SecretGameDomainException {

    public NoUnusedAcceptedWordsForHintException(String roomCode) {
        super("NO_HINT_WORDS_LEFT", "No unused accepted words left to reveal as a hint in room: " + roomCode,
                Map.of("roomCode", roomCode));
    }
}
