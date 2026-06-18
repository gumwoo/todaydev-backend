package com.todaydev.notification.infrastructure.queue;

import reactor.core.publisher.Mono;

public interface NotificationPublisher {

    Mono<Void> publish(NotificationQueueMessage message);

    Mono<Void> publishRetry(NotificationQueueMessage message);
}
