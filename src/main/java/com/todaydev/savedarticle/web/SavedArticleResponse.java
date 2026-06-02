package com.todaydev.savedarticle.web;

import com.todaydev.briefing.domain.Source;
import java.time.LocalDateTime;

public record SavedArticleResponse(
        Long savedId,
        Long itemId,
        String title,
        String url,
        Source source,
        String memo,
        LocalDateTime savedAt
) {
}
