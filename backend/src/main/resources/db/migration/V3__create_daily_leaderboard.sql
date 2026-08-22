CREATE TABLE daily_leaderboard_entries (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    challenge_date DATE NOT NULL,
    word_count INT NOT NULL,
    accuracy_percent INT NOT NULL,
    completed_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_daily_leaderboard_user_date UNIQUE (user_id, challenge_date)
);

CREATE INDEX idx_daily_leaderboard_date_rank
    ON daily_leaderboard_entries (challenge_date, word_count ASC, accuracy_percent DESC);
