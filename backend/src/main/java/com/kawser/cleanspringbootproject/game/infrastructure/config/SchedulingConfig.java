package com.kawser.cleanspringbootproject.game.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * A single shared thread pool backs both the per-room phase timers
 * (SpringTaskSchedulerPhaseTimer) and the periodic room cleanup sweep
 * (RoomCleanupTask, via @Scheduled) — one pool is enough at this scale and
 * avoids silently running two independent pools.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(8);
        scheduler.setThreadNamePrefix("game-scheduler-");
        scheduler.initialize();
        return scheduler;
    }
}
