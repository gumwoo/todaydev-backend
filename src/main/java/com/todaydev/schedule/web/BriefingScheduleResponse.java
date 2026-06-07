package com.todaydev.schedule.web;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record BriefingScheduleResponse(
        @JsonFormat(pattern = "HH:mm")
        LocalTime briefingTime,
        String timezone,
        boolean enabled,
        LocalDateTime updatedAt
) {
}
