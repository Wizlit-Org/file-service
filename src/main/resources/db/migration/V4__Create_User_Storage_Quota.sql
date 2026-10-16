CREATE TABLE IF NOT EXISTS user_storage_quota (
    user_id BIGINT NOT NULL,
    year_month VARCHAR(6) NOT NULL,
    used_bytes BIGINT NOT NULL DEFAULT 0,
    max_bytes BIGINT NOT NULL DEFAULT 104857600,
    updated_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, year_month)
);
