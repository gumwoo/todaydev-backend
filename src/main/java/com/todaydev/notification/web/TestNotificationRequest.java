package com.todaydev.notification.web;

import com.todaydev.notification.domain.NotificationChannel;
import jakarta.validation.constraints.NotNull;

public record TestNotificationRequest(
        @NotNull NotificationChannel channel
) {
}
