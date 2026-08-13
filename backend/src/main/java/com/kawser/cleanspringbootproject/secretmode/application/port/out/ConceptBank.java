package com.kawser.cleanspringbootproject.secretmode.application.port.out;

import com.kawser.cleanspringbootproject.secretmode.domain.model.Concept;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Resolves which Concept is today's secret-mode daily challenge, and looks
 * one up by id. Mirrors the role
 * game.application.port.out.ChainWordBank/CuratedDailyChainWordBank play
 * for the original mode's daily challenge, but concepts are looked up
 * whole (id + hidden quality + word bank) rather than dealt out
 * word-by-word.
 */
public interface ConceptBank {

    Concept dailyConceptFor(LocalDate utcDate);

    Optional<Concept> findById(String id);
}
