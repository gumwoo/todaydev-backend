package com.todaydev.briefing.progress;

import com.todaydev.briefing.domain.BriefingStatus;
import com.todaydev.briefing.domain.Source;
import java.util.List;

public record BriefingProgressPayload(
        Long briefingId,
        ProgressStep step,
        Source source,
        Integer processed,
        Integer total,
        String message,
        BriefingStatus status,
        List<Source> failedSources
) {

    public static BriefingProgressPayload progress(
            Long briefingId,
            ProgressStep step,
            Source source,
            Integer processed,
            Integer total,
            String message
    ) {
        return new BriefingProgressPayload(briefingId, step, source, processed, total, message, null, List.of());
    }

    public static BriefingProgressPayload terminal(
            Long briefingId,
            ProgressStep step,
            BriefingStatus status,
            String message,
            List<Source> failedSources
    ) {
        return new BriefingProgressPayload(briefingId, step, null, null, null, message, status, failedSources);
    }
}
