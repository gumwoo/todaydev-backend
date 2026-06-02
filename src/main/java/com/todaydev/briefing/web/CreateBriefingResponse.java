package com.todaydev.briefing.web;

import com.todaydev.briefing.domain.BriefingStatus;
import java.time.LocalDateTime;

public record CreateBriefingResponse(
        Long briefingId,
        BriefingStatus status,
        LocalDateTime createdAt
) {
}
