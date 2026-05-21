package com.todaydev.common.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "external")
public record ExternalApiProperties(
        @Valid GitHub github,
        @Valid HackerNews hackernews,
        @Valid DevTo devto,
        @Valid Gemini gemini
) {

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
            @NotBlank String baseUrl
    ) {
    }
}
