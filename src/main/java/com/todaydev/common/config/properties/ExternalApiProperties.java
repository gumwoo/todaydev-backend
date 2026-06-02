package com.todaydev.common.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "external")
public record ExternalApiProperties(
        @Valid Client client,
        @Valid GitHub github,
        @Valid HackerNews hackernews,
        @Valid DevTo devto,
        @Valid Gemini gemini
) {

    public record Client(
            @Min(100) long timeoutMillis,
            @Min(1) int retryMaxAttempts,
            @Min(0) long retryBackoffMillis
    ) {

        public Duration timeout() {
            return Duration.ofMillis(timeoutMillis);
        }

        public Duration retryBackoff() {
            return Duration.ofMillis(retryBackoffMillis);
        }
    }

    public record GitHub(
            String token,
            @NotBlank String baseUrl
    ) {
    }

    public record HackerNews(
            @NotBlank String baseUrl
    ) {
    }

    public record DevTo(
            @NotBlank String baseUrl
    ) {
    }

    public record Gemini(
            String apiKey,
            @NotBlank String baseUrl,
            @NotBlank String model
    ) {
    }
}
