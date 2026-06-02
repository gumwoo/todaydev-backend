package com.todaydev.briefing.repository;

import com.todaydev.briefing.domain.BriefingStatus;
import java.time.LocalDateTime;

public record BriefingListItem(
        Long briefingId,
        String title,
        String summary,
        BriefingStatus status,
        LocalDateTime generatedAt,
        long itemCount
) {
}
