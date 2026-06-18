package com.todaydev.notification.infrastructure.discord;

import reactor.core.publisher.Mono;

public interface DiscordNotificationSender {

    Mono<Void> send(String webhookUrl, DiscordWebhookPayload payload);
}
