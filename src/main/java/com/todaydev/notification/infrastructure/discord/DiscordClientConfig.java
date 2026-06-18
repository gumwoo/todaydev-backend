package com.todaydev.notification.infrastructure.discord;

import com.todaydev.common.config.properties.NotificationProperties;
import com.todaydev.common.exception.ErrorCode;
import com.todaydev.common.exception.TodaydevException;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class DiscordClientConfig implements DiscordNotificationSender {

    private final WebClient webClient;
    private final NotificationProperties properties;

    public DiscordClientConfig(@Qualifier("discordWebClient") WebClient webClient, NotificationProperties properties) {
        this.webClient = webClient;
        this.properties = properties;
    }

    @Override
    public Mono<Void> send(String webhookUrl, DiscordWebhookPayload payload) {
        if (!properties.enabled() || !properties.discord().enabled()) {
            return Mono.empty();
        }

        return webClient.post()
                .uri(webhookUrl)
                .bodyValue(payload)
                .retrieve()
                .onStatus(status -> status.value() == 429,
                        response -> Mono.error(new TodaydevException(ErrorCode.NOTIFICATION_RATE_LIMITED)))
                .onStatus(status -> status.isError(),
                        response -> Mono.error(new TodaydevException(ErrorCode.NOTIFICATION_SEND_FAILED)))
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(properties.discord().timeoutMillis()))
                .then()
                .onErrorMap(throwable -> throwable instanceof TodaydevException
                        ? throwable
                        : new TodaydevException(ErrorCode.NOTIFICATION_SEND_FAILED));
    }
}
