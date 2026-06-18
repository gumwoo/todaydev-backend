package com.todaydev.notification.infrastructure.email;

import reactor.core.publisher.Mono;

public interface EmailNotificationSender {

    Mono<Void> send(EmailNotificationPayload payload);
}
