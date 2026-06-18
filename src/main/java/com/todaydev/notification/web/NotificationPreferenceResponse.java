package com.todaydev.notification.web;

import com.todaydev.notification.domain.NotificationChannel;
import java.time.LocalDateTime;

public record NotificationPreferenceResponse(
        NotificationChannel channel,
        boolean enabled,
        boolean configured,
        LocalDateTime updatedAt
) {
}
