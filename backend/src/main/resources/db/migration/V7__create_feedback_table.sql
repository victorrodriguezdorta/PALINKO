-- One row per account and notification topic. "topic" names which card or
-- notification the row belongs to (e.g. CREATOR_LETTER), so future
-- notifications can reuse this table to remember who dismissed or answered
-- them.
CREATE TABLE feedback (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    username VARCHAR(15) NOT NULL,
    topic VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('DISMISSED', 'ANSWERED')),
    suggestion TEXT,
    issues TEXT,
    dismissed_on DATE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_feedback_user_topic UNIQUE (user_id, topic)
);

CREATE INDEX idx_feedback_topic_status ON feedback (topic, status);
