package com.todaydev.external;

import java.time.OffsetDateTime;
import java.util.Map;

public record ExternalArticle(
        ExternalSource source,
        String externalId,
        String title,
        String url,
        String summary,
        OffsetDateTime publishedAt,
        Map<String, Object> metadata
) {

    public ExternalArticle {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
