package com.todaydev.briefing.domain;

import java.math.BigDecimal;

public record BriefingItem(
        Long itemId,
        Long briefingId,
        Source source,
        String externalId,
        String title,
        String url,
        String summary,
        BigDecimal score
) {
}
