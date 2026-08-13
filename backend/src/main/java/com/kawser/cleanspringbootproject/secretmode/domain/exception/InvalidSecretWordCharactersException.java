package com.kawser.cleanspringbootproject.secretmode.domain.exception;

import java.util.Map;

public class InvalidSecretWordCharactersException extends SecretGameDomainException {

    public InvalidSecretWordCharactersException(String word) {
        super("INVALID_SECRET_WORD_CHARACTERS", "Invalid word characters: " + word, Map.of("word", word));
    }
}
