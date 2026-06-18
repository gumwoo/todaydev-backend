package com.todaydev.notification.infrastructure.queue;

public record NotificationQueueHeaders(
        String messageVersion,
        String traceId
) {
}
