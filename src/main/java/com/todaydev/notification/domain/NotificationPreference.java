package com.todaydev.notification.domain;

import java.time.LocalDateTime;

public record NotificationPreference(
        Long preferenceId,
        Long userId,
        NotificationChannel channel,
        String destination,
        boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
