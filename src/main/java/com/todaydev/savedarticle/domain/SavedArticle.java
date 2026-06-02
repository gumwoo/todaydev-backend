package com.todaydev.savedarticle.domain;

import com.todaydev.briefing.domain.Source;
import java.time.LocalDateTime;

public record SavedArticle(
        Long savedId,
        Long userId,
        Long itemId,
        String title,
        String url,
        Source source,
        String memo,
        LocalDateTime savedAt
) {
}
