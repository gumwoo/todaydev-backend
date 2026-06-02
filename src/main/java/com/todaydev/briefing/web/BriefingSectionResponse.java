package com.todaydev.briefing.web;

import com.todaydev.briefing.domain.BriefingStatus;
import com.todaydev.briefing.domain.Source;
import java.util.List;

public record BriefingSectionResponse(
        Source source,
        BriefingStatus status,
        List<BriefingItemResponse> items
) {
}
