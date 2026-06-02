package com.todaydev.preference.domain;

import java.time.LocalDateTime;

public record InterestKeyword(
        Long keywordId,
        Long userId,
        String keyword,
        int weight,
        LocalDateTime createdAt
) {
}
