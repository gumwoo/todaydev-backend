package com.todaydev.notification.infrastructure.email;

public record EmailNotificationPayload(
        String to,
        String subject,
        String body
) {
}
