package com.todaydev.briefing.web;

import com.todaydev.briefing.domain.BriefingStatus;
import java.time.LocalDateTime;
import java.util.List;

public record BriefingDetailResponse(
        Long briefingId,
        String title,
        String summary,
        BriefingStatus status,
        LocalDateTime generatedAt,
        List<BriefingSectionResponse> sections
) {
}
