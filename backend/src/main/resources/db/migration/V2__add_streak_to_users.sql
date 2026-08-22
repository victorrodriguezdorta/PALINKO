ALTER TABLE users
    ADD COLUMN current_streak INT NOT NULL DEFAULT 0;

ALTER TABLE users
    ADD COLUMN last_streak_date DATE;
