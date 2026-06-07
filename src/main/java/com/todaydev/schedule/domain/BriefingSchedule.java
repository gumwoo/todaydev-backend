package com.todaydev.schedule.domain;

import java.time.LocalDateTime;
import java.time.LocalTime;

public record BriefingSchedule(
        Long userId,
        LocalTime briefingTime,
        String timezone,
        boolean enabled,
        LocalDateTime updatedAt
) {
    public static final LocalTime DEFAULT_BRIEFING_TIME = LocalTime.of(8, 0);
    public static final String DEFAULT_TIMEZONE = "Asia/Seoul";
}
