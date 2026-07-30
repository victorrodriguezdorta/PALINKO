package com.kawser.cleanspringbootproject.game.infrastructure.adapter.in.rest.dto;

import com.kawser.cleanspringbootproject.game.domain.model.GameLanguage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record WordRelationRequest(
        @NotBlank(message = "wordA cannot be blank") String wordA,
        @NotBlank(message = "wordB cannot be blank") String wordB,
        @NotNull(message = "language is required") GameLanguage language) {
}
