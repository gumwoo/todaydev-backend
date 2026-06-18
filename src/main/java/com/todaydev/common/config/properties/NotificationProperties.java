package com.todaydev.common.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "notification")
public record NotificationProperties(
        boolean enabled,
        @Min(1) int maxItemsPerMessage,
        @Valid Queue queue,
        @Valid Email email,
        @Valid Slack slack,
        @Valid Discord discord
) {
    public record Queue(
        @NotBlank String provider,
        @Min(100) long publishTimeoutMillis,
        @Min(1) int consumerConcurrency,
        @Min(1) int retryMaxAttempts,
        @Min(0) long retryBackoffMillis,
        boolean dlqEnabled,
        @Valid Kafka kafka,
        @Valid RabbitMq rabbitmq
    ) {}

    public record Kafka(
        @NotBlank String bootstrapServers,
        @NotBlank String requestTopic,
        @NotBlank String retryTopic,
        @NotBlank String dlqTopic,
        @NotBlank String consumerGroupId
    ) {}

    public record RabbitMq(
        @NotBlank String host,
        @Min(1) int port,
        @NotBlank String username,
        @NotBlank String password,
        @NotBlank String exchange,
        @NotBlank String requestQueue,
        @NotBlank String retryQueue,
        @NotBlank String dlq
    ) {}

    public record Email(
        boolean enabled,
        @NotBlank String from,
        @NotBlank String subjectPrefix,
        @Min(100) long timeoutMillis
    ) {}

    public record Slack(
        boolean enabled,
        @NotBlank String baseUrl,
        @Min(100) long timeoutMillis
    ) {}

    public record Discord(
        boolean enabled,
        @NotBlank String baseUrl,
        @Min(100) long timeoutMillis
    ) {}
}
