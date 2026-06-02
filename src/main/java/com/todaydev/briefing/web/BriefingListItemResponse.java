package com.todaydev.briefing.web;

import com.todaydev.briefing.domain.BriefingStatus;
import java.time.LocalDateTime;

public record BriefingListItemResponse(
        Long briefingId,
        String title,
        String summary,
        BriefingStatus status,
        LocalDateTime generatedAt,
        long itemCount
) {
}
