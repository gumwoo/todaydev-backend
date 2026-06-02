package com.todaydev.ai;

import com.todaydev.briefing.domain.ApiCallLog;

public record AiSummaryResult(
        String summary,
        boolean success,
        ApiCallLog apiCallLog
) {
}
