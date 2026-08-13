package com.kawser.cleanspringbootproject.secretmode.domain.exception;

import java.util.Map;

public class InvalidSolutionTextException extends SecretGameDomainException {

    public InvalidSolutionTextException(String formulationText) {
        super("INVALID_SOLUTION_TEXT", "Invalid solution formulation", Map.of("formulationText",
                formulationText == null ? "" : formulationText));
    }
}
