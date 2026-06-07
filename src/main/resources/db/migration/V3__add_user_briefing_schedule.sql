CREATE TABLE user_briefing_schedule (
    user_id        BIGINT       PRIMARY KEY REFERENCES users(user_id) ON DELETE CASCADE,
    briefing_time  TIME         NOT NULL DEFAULT '08:00',
    timezone       VARCHAR(64)  NOT NULL DEFAULT 'Asia/Seoul',
    enabled        BOOLEAN      NOT NULL DEFAULT TRUE,
    updated_at     TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_user_briefing_schedule_enabled
    ON user_briefing_schedule (enabled, briefing_time);

INSERT INTO user_briefing_schedule (user_id)
SELECT user_id
FROM users
ON CONFLICT (user_id) DO NOTHING;
