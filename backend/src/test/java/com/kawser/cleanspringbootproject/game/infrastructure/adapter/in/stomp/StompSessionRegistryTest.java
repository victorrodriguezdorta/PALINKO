package com.kawser.cleanspringbootproject.game.infrastructure.adapter.in.stomp;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StompSessionRegistryTest {

    @Test
    void sessionIdsForReturnsEveryLiveSessionOfThatPlayerInThatRoom() {
        StompSessionRegistry registry = new StompSessionRegistry();
        registry.register("session-1", "ROOM1", "player-1", "token-1");
        registry.register("session-2", "ROOM1", "player-1", "token-1");
        registry.register("session-3", "ROOM1", "player-2", "token-2");
        registry.register("session-4", "ROOM2", "player-1", "token-3");

        assertThat(registry.sessionIdsFor("ROOM1", "player-1")).containsExactlyInAnyOrder("session-1", "session-2");
        assertThat(registry.sessionIdsFor("ROOM1", "player-2")).containsExactly("session-3");
        assertThat(registry.sessionIdsFor("ROOM2", "player-1")).containsExactly("session-4");
        assertThat(registry.sessionIdsFor("ROOM1", "player-3")).isEmpty();
    }

    @Test
    void removeDropsTheSessionFromFutureLookups() {
        StompSessionRegistry registry = new StompSessionRegistry();
        registry.register("session-1", "ROOM1", "player-1", "token-1");

        registry.remove("session-1");

        assertThat(registry.sessionIdsFor("ROOM1", "player-1")).isEmpty();
        assertThat(registry.find("session-1")).isEmpty();
    }
}
