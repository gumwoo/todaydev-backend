package com.todaydev.preference.web;

import java.time.LocalDateTime;

public record KeywordResponse(
        Long keywordId,
        String keyword,
        int weight,
        LocalDateTime createdAt
) {
}
