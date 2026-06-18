package com.todaydev.notification.infrastructure.slack;

import com.todaydev.common.config.properties.NotificationProperties;
import com.todaydev.common.exception.ErrorCode;
import com.todaydev.common.exception.TodaydevException;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class SlackClientConfig implements SlackNotificationSender {

    private final WebClient webClient;
    private final NotificationProperties properties;

    public SlackClientConfig(@Qualifier("slackWebClient") WebClient webClient, NotificationProperties properties) {
        this.webClient = webClient;
        this.properties = properties;
    }

    @Override
    public Mono<Void> send(String webhookUrl, SlackWebhookPayload payload) {
        if (!properties.enabled() || !properties.slack().enabled()) {
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
                .timeout(Duration.ofMillis(properties.slack().timeoutMillis()))
                .then()
                .onErrorMap(throwable -> throwable instanceof TodaydevException
                        ? throwable
                        : new TodaydevException(ErrorCode.NOTIFICATION_SEND_FAILED));
    }
}
