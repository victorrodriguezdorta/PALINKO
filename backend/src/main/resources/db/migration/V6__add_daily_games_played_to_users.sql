ALTER TABLE users
    ADD COLUMN daily_games_played INT NOT NULL DEFAULT 0
    CHECK (daily_games_played >= 0);

-- Existing history stores at most one row per account and challenge date.
UPDATE users u
SET daily_games_played = (
    SELECT CAST(COUNT(*) AS INTEGER)
    FROM history h
    WHERE h.user_id = u.id
);
