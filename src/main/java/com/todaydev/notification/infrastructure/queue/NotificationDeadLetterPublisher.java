package com.todaydev.notification.infrastructure.queue;

import reactor.core.publisher.Mono;

public interface NotificationDeadLetterPublisher {

    Mono<Void> publish(NotificationQueueMessage message, Throwable cause);
}
