package com.todaydev.briefing.progress;

public record BriefingStreamMessage(
        String eventName,
        BriefingProgressPayload payload
) {

    public static final String PROGRESS_EVENT = "BRIEFING_PROGRESS";
    public static final String DONE_EVENT = "BRIEFING_DONE";
    public static final String PARTIAL_DONE_EVENT = "BRIEFING_PARTIAL_DONE";
    public static final String FAILED_EVENT = "BRIEFING_FAILED";

    public boolean terminal() {
        return !PROGRESS_EVENT.equals(eventName);
    }
}
