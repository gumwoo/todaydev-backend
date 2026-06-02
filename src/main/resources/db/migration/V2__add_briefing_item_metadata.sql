ALTER TABLE briefing_item
    ADD COLUMN published_at TIMESTAMP,
    ADD COLUMN metadata JSONB NOT NULL DEFAULT '{}'::jsonb;

CREATE INDEX idx_briefing_item_published_at ON briefing_item (briefing_id, published_at DESC);
