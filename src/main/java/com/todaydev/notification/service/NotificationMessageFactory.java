package com.todaydev.notification.service;

import com.todaydev.common.trace.TraceIds;
import com.todaydev.notification.domain.NotificationDelivery;
import com.todaydev.notification.domain.NotificationMessage;
import com.todaydev.notification.infrastructure.queue.NotificationQueueMessage;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class NotificationMessageFactory {

    public NotificationMessage create(NotificationDelivery delivery, int attempt) {
        return new NotificationMessage(
                UUID.randomUUID().toString(),
                delivery.deliveryId(),
                delivery.briefingId(),
                delivery.userId(),
                delivery.channel(),
                attempt,
                OffsetDateTime.now(),
                TraceIds.from(null)
        );
    }

    public NotificationQueueMessage toQueueMessage(NotificationMessage message) {
        return new NotificationQueueMessage(
                message.messageId(),
                message.deliveryId(),
                message.briefingId(),
                message.userId(),
                message.channel(),
                message.attempt(),
                message.createdAt(),
                message.traceId()
        );
    }
}
