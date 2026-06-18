package com.todaydev.notification.domain;

import java.time.LocalDateTime;

public record NotificationDelivery(
        Long deliveryId,
        Long userId,
        Long briefingId,
        NotificationChannel channel,
        NotificationDeliveryStatus status,
        int attemptCount,
        String lastErrorCode,
        String lastErrorMessage,
        LocalDateTime queuedAt,
        LocalDateTime sentAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
