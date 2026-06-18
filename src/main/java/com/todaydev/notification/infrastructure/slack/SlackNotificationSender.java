package com.todaydev.notification.infrastructure.slack;

import reactor.core.publisher.Mono;

public interface SlackNotificationSender {

    Mono<Void> send(String webhookUrl, SlackWebhookPayload payload);
}
