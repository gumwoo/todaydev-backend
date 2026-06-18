package com.todaydev.notification.web;

import com.todaydev.notification.domain.NotificationChannel;
import com.todaydev.notification.domain.NotificationDeliveryStatus;
import java.time.LocalDateTime;

public record NotificationDeliveryResponse(
        Long deliveryId,
        Long briefingId,
        NotificationChannel channel,
        NotificationDeliveryStatus status,
        int attemptCount,
        LocalDateTime queuedAt,
        LocalDateTime sentAt,
        LocalDateTime updatedAt
) {
}
