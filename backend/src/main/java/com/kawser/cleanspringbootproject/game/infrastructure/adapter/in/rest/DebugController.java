package com.kawser.cleanspringbootproject.game.infrastructure.adapter.in.rest;

import com.kawser.cleanspringbootproject.game.application.port.out.WordRelation;
import com.kawser.cleanspringbootproject.game.application.port.out.WordRelationChecker;
import com.kawser.cleanspringbootproject.game.infrastructure.adapter.in.rest.dto.WordRelationRequest;
import com.kawser.cleanspringbootproject.game.infrastructure.adapter.in.rest.dto.WordRelationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Debug-only endpoint exposing the same WordRelationChecker the game uses
 * internally to score answers, so the relatedness between two arbitrary
 * words can be inspected directly without going through a full game round.
 */
@RestController
@RequestMapping("/debug")
public class DebugController {

    private final WordRelationChecker wordRelationChecker;

    public DebugController(WordRelationChecker wordRelationChecker) {
        this.wordRelationChecker = wordRelationChecker;
    }

    @Operation(summary = "Calculate the relatedness percentage between two words")
    @ApiResponse(responseCode = "200", description = "Relatedness percentage calculated")
    @PostMapping("/word-relation")
    public ResponseEntity<WordRelationResponse> wordRelation(@RequestBody @Valid WordRelationRequest request) {
        WordRelation relation = wordRelationChecker.relatedness(request.wordA(), request.wordB(), request.language());
        return ResponseEntity.ok(new WordRelationResponse(relation.percentage(), relation.justification()));
    }
}
