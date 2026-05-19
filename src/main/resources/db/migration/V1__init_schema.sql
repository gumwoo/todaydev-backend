-- =============================================
-- V1: 초기 스키마 생성
-- progress_event 테이블은 MVP에서 제외 (Redis TTL로 대체)
-- =============================================

CREATE TABLE users (
    user_id     BIGSERIAL       PRIMARY KEY,
    email       VARCHAR(255)    NOT NULL,
    password_hash VARCHAR(255)  NOT NULL,
    created_at  TIMESTAMP       NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE TABLE user_interest (
    interest_id BIGSERIAL       PRIMARY KEY,
    user_id     BIGINT          NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    keyword     VARCHAR(100)    NOT NULL,
    weight      SMALLINT        NOT NULL DEFAULT 1,
    created_at  TIMESTAMP       NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_interest UNIQUE (user_id, keyword)
);

CREATE TABLE watched_repository (
    repo_id     BIGSERIAL       PRIMARY KEY,
    user_id     BIGINT          NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    owner       VARCHAR(100)    NOT NULL,
    repo_name   VARCHAR(100)    NOT NULL,
    created_at  TIMESTAMP       NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_watched_repo UNIQUE (user_id, owner, repo_name)
);

-- status 확정값: GENERATING | COMPLETED | PARTIAL | SUMMARY_FAILED | FAILED
CREATE TABLE briefing (
    briefing_id  BIGSERIAL      PRIMARY KEY,
    user_id      BIGINT         NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    title        VARCHAR(500),
    summary      TEXT,
    status       VARCHAR(20)    NOT NULL DEFAULT 'GENERATING',
    generated_at TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_briefing_user_date ON briefing (user_id, generated_at DESC);

CREATE TABLE briefing_item (
    item_id      BIGSERIAL      PRIMARY KEY,
    briefing_id  BIGINT         NOT NULL REFERENCES briefing(briefing_id) ON DELETE CASCADE,
    source       VARCHAR(20)    NOT NULL,   -- GITHUB | HACKER_NEWS | DEVTO
    external_id  VARCHAR(255),
    title        VARCHAR(1000)  NOT NULL,
    url          VARCHAR(2000)  NOT NULL,
    summary      TEXT,
    score        NUMERIC(6, 2)  NOT NULL DEFAULT 0
);

CREATE INDEX idx_briefing_item_score ON briefing_item (briefing_id, score DESC);

CREATE TABLE api_call_log (
    log_id       BIGSERIAL      PRIMARY KEY,
    briefing_id  BIGINT         REFERENCES briefing(briefing_id) ON DELETE SET NULL,
    source       VARCHAR(20)    NOT NULL,
    status       VARCHAR(10)    NOT NULL,   -- SUCCESS | PARTIAL | FAILED
    latency_ms   INT,
    error_message TEXT,
    called_at    TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_api_call_log ON api_call_log (briefing_id, source);

CREATE TABLE saved_article (
    saved_id    BIGSERIAL       PRIMARY KEY,
    user_id     BIGINT          NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    item_id     BIGINT          NOT NULL REFERENCES briefing_item(item_id) ON DELETE CASCADE,
    memo        TEXT,
    created_at  TIMESTAMP       NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_saved_article UNIQUE (user_id, item_id)
);
