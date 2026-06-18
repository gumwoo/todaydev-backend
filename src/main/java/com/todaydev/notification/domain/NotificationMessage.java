package com.todaydev.notification.domain;

import java.time.OffsetDateTime;

public record NotificationMessage(
        String messageId,
        Long deliveryId,
        Long briefingId,
        Long userId,
        NotificationChannel channel,
        int attempt,
        OffsetDateTime createdAt,
        String traceId
) {
}
