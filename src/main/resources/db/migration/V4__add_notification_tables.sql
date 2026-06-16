CREATE TABLE notification_preference (
    preference_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    channel VARCHAR(20) NOT NULL,
    destination VARCHAR(2000) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_notification_preference UNIQUE (user_id, channel)
);

CREATE TABLE notification_delivery (
    delivery_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    briefing_id BIGINT NOT NULL REFERENCES briefing(briefing_id) ON DELETE CASCADE,
    channel VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    last_error_code VARCHAR(100),
    last_error_message TEXT,
    queued_at TIMESTAMP,
    sent_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_notification_delivery UNIQUE (user_id, briefing_id, channel)
);

CREATE INDEX idx_notification_delivery_user_created
    ON notification_delivery (user_id, created_at DESC);

CREATE INDEX idx_notification_delivery_status
    ON notification_delivery (status, updated_at);