package com.kawser.cleanspringbootproject.game.infrastructure.adapter.out.scheduling;

import com.kawser.cleanspringbootproject.game.application.dto.AdvancePhaseCommand;
import com.kawser.cleanspringbootproject.game.application.port.in.AdvancePhaseUseCase;
import com.kawser.cleanspringbootproject.game.application.port.out.PhaseScheduler;
import com.kawser.cleanspringbootproject.game.domain.model.RoundPhase;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * Bridges the application-level PhaseScheduler port to Spring's
 * TaskScheduler. Keeps at most one pending future per room: scheduling a
 * new advance for a room implicitly cancels whatever was previously
 * pending for it, since AdvancePhaseCommand's roundNumber/expectedPhase
 * check in GameApplicationService already treats a stale fire as a no-op,
 * this cancellation is a courtesy to avoid piling up dead futures, not a
 * correctness requirement.
 *
 * AdvancePhaseUseCase is looked up lazily through an ObjectProvider rather
 * than injected directly in the constructor: GameApplicationService (the
 * bean that implements AdvancePhaseUseCase) itself depends on
 * PhaseScheduler, so a direct constructor cycle between the two beans is
 * unresolvable by Spring. Neither side needs the other before the timer
 * actually fires, so deferring the lookup to call time breaks the cycle
 * without changing either component's public contract.
 */
@Component
public class SpringTaskSchedulerPhaseTimer implements PhaseScheduler {

    private final TaskScheduler taskScheduler;
    private final ObjectProvider<AdvancePhaseUseCase> advancePhaseUseCaseProvider;
    private final Map<String, ScheduledFuture<?>> pendingByRoom = new ConcurrentHashMap<>();

    public SpringTaskSchedulerPhaseTimer(
            TaskScheduler taskScheduler, ObjectProvider<AdvancePhaseUseCase> advancePhaseUseCaseProvider) {
        this.taskScheduler = taskScheduler;
        this.advancePhaseUseCaseProvider = advancePhaseUseCaseProvider;
    }

    @Override
    public void scheduleAdvance(String roomCode, int roundNumber, RoundPhase expectedPhase, Instant fireAt) {
        cancel(roomCode);
        ScheduledFuture<?> future = taskScheduler.schedule(
                () -> advancePhaseUseCaseProvider.getObject()
                        .forceAdvance(new AdvancePhaseCommand(roomCode, roundNumber, expectedPhase)),
                fireAt);
        pendingByRoom.put(roomCode, future);
    }

    @Override
    public void cancel(String roomCode) {
        ScheduledFuture<?> future = pendingByRoom.remove(roomCode);
        if (future != null) {
            future.cancel(false);
        }
    }
}
