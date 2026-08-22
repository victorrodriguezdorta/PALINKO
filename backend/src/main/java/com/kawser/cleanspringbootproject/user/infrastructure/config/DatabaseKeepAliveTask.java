package com.kawser.cleanspringbootproject.user.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Pings the database on a short fixed interval so a cloud Postgres provider
 * with an auto-suspend/scale-to-zero free tier (e.g. Neon, which suspends
 * its compute endpoint after 5 minutes of inactivity) never sees a gap long
 * enough to go idle. Without this, the backend itself can stay up 24/7 on a
 * paid Render plan while every request still pays a ~1s cold-start penalty
 * on the database side after any quiet period.
 */
@Component
public class DatabaseKeepAliveTask {

    private static final Logger log = LoggerFactory.getLogger(DatabaseKeepAliveTask.class);

    private final JdbcTemplate jdbcTemplate;

    public DatabaseKeepAliveTask(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Scheduled(fixedRateString = "${app.db.keep-alive.interval-seconds:240}000")
    public void ping() {
        try {
            jdbcTemplate.execute("SELECT 1");
        } catch (Exception e) {
            log.warn("Database keep-alive ping failed: {}", e.getMessage());
        }
    }
}
