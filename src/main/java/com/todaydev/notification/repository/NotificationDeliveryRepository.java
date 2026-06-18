package com.todaydev.notification.repository;

import com.todaydev.notification.domain.NotificationChannel;
import com.todaydev.notification.domain.NotificationDelivery;
import com.todaydev.notification.domain.NotificationDeliveryStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface NotificationDeliveryRepository {

    Mono<NotificationDelivery> createPending(Long userId, Long briefingId, NotificationChannel channel);

    Mono<NotificationDelivery> findById(Long deliveryId);

    Flux<NotificationDelivery> findByUserId(Long userId, int page, int size);

    Mono<Long> countByUserId(Long userId);

    Mono<NotificationDelivery> updateStatus(
            Long deliveryId,
            NotificationDeliveryStatus status,
            int attemptCount,
            String errorCode,
            String errorMessage
    );

    Mono<NotificationDelivery> markPublished(Long deliveryId);

    Mono<NotificationDelivery> markSending(Long deliveryId, int attemptCount);

    Mono<NotificationDelivery> markSent(Long deliveryId, int attemptCount);
}
