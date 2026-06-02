package com.todaydev.briefing.domain;

import java.time.LocalDateTime;

public record Briefing(
        Long briefingId,
        Long userId,
        String title,
        String summary,
        BriefingStatus status,
        LocalDateTime generatedAt
) {

    public static Briefing generating(Long userId, LocalDateTime generatedAt) {
        return new Briefing(null, userId, null, null, BriefingStatus.GENERATING, generatedAt);
    }
}
