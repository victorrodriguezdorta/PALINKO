CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(15) NOT NULL,
    password TEXT NOT NULL,
    role VARCHAR(20) NOT NULL,
    email VARCHAR(255) NOT NULL,
    avatar_seed VARCHAR(64) NOT NULL,
    is_enabled BOOLEAN NOT NULL DEFAULT false,
    otp VARCHAR(6),
    otp_generation_time TIMESTAMP
);

CREATE UNIQUE INDEX idx_users_username ON users (username);
CREATE UNIQUE INDEX idx_users_email ON users (email);
