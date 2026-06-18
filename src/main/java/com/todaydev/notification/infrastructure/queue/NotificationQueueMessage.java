package com.todaydev.notification.infrastructure.queue;

import com.todaydev.notification.domain.NotificationChannel;
import java.time.OffsetDateTime;

public record NotificationQueueMessage(
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
