package com.todaydev.external.hackernews;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

record HackerNewsItemResponse(
        Long id,
        String title,
        String url,
        String by,
        Integer score,
        Long time,
        String type
) {

    OffsetDateTime publishedAt() {
        if (time == null) {
            return null;
        }
        return OffsetDateTime.ofInstant(Instant.ofEpochSecond(time), ZoneOffset.UTC);
    }
}
