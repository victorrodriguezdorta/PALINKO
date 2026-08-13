package com.kawser.cleanspringbootproject.secretmode.application.service;

import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Today's UTC date, used to pick the daily Concept deterministically so
 * every player who opens the daily challenge on the same calendar date
 * gets the same one. Kept as its own trivial utility rather than reusing
 * game.application.service.DailySeed: that class is package-private and
 * secretmode only ever needs the date itself (ConceptBank indexes by day
 * of year, not by a hash-derived numeric seed), so duplicating one line is
 * simpler and keeps the two modes from depending on each other's internals.
 */
final class SecretDailySeed {

    private SecretDailySeed() {
    }

    static LocalDate today() {
        return LocalDate.now(ZoneOffset.UTC);
    }
}
