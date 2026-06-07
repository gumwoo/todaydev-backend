package com.todaydev.schedule.web;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

public record BriefingScheduleRequest(
        @NotNull
        @JsonFormat(pattern = "HH:mm")
        LocalTime briefingTime,

        @NotBlank
        String timezone,

        @NotNull
        Boolean enabled
) {
}
