package com.todaydev.briefing.web;

import com.todaydev.briefing.domain.Source;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public record BriefingItemResponse(
        Long itemId,
        Source source,
        String externalId,
        String title,
        String url,
        String summary,
        BigDecimal score,
        LocalDateTime publishedAt,
        Map<String, Object> metadata,
        boolean saved
) {
}
