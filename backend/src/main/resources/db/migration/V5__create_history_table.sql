CREATE TABLE history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    username VARCHAR(15) NOT NULL,
    "day" DATE NOT NULL,
    word_chain TEXT NOT NULL,
    CONSTRAINT uq_history_user_date UNIQUE (user_id, "day")
);

CREATE INDEX idx_history_user_date ON history (user_id, "day" DESC);
