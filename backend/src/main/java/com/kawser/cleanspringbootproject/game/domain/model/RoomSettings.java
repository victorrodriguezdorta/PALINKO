package com.kawser.cleanspringbootproject.game.domain.model;

/**
 * Per-room configuration, editable by the host only from inside the LOBBY
 * (see Room.updateSettings) — a room is always created with the fixed
 * defaults below, precisely so the create-room step never has to expose
 * these as editable fields itself.
 */
public record RoomSettings(
        int wordTimeSeconds, int voteTimeSeconds, GameLanguage language, int infiltratorCount,
        int phaseCount, boolean daily) {

    private static final int DEFAULT_WORD_TIME_SECONDS = 45;
    private static final int DEFAULT_VOTE_TIME_SECONDS = 30;
    private static final int DEFAULT_PHASE_COUNT = 3;
    public static final int MAX_PHASE_COUNT = 10;

    public RoomSettings {
        if (wordTimeSeconds < 1) {
            throw new IllegalArgumentException("wordTimeSeconds must be at least 1");
        }
        if (voteTimeSeconds < 1) {
            throw new IllegalArgumentException("voteTimeSeconds must be at least 1");
        }
        if (language == null) {
            throw new IllegalArgumentException("language must not be null");
        }
        if (infiltratorCount < 0) {
            throw new IllegalArgumentException("infiltratorCount must not be negative");
        }
        if (phaseCount < 1 || phaseCount > MAX_PHASE_COUNT) {
            throw new IllegalArgumentException("phaseCount must be between 1 and " + MAX_PHASE_COUNT);
        }
    }

    /**
     * Timers/turns always start at their fixed defaults; language is the
     * one setting the create-room step actually needs to supply (it's the
     * host's own chosen language, not an arbitrary rule to tune later) —
     * though, like the other settings, the host can still change it from
     * the lobby via Room.updateSettings before the game starts.
     * infiltratorCount always starts at 0 here, since a room is created
     * with just its host (1 player): Room.reapplyAutomaticInfiltratorCount
     * immediately raises it to a third of the headcount as more players
     * join, until the host sets it explicitly. phaseCount defaults to 3,
     * unless the host lowers or raises it from the lobby.
     */
    public static RoomSettings defaults(GameLanguage language) {
        return new RoomSettings(
                DEFAULT_WORD_TIME_SECONDS, DEFAULT_VOTE_TIME_SECONDS, language, 0, DEFAULT_PHASE_COUNT, false);
    }

    /**
     * A daily-challenge room's settings: word/vote timers are irrelevant
     * (the application layer never schedules a PhaseScheduler timeout for a
     * daily room, see GameApplicationService), there is never an
     * infiltrator (a solo player always plays cooperatively), and
     * phaseCount is fixed rather than host-chosen. daily=true also blocks
     * Room.updateSettings-style edits in practice, since a daily room is
     * started immediately at creation and never sits in LOBBY.
     */
    public static RoomSettings daily(GameLanguage language, int phaseCount) {
        return new RoomSettings(
                DEFAULT_WORD_TIME_SECONDS, DEFAULT_VOTE_TIME_SECONDS, language, 0, phaseCount, true);
    }
}
