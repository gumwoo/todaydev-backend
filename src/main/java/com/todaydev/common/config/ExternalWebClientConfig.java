package com.todaydev.common.config;

import com.todaydev.common.config.properties.ExternalApiProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class ExternalWebClientConfig {

    @Bean
    @Qualifier("githubWebClient")
    public WebClient githubWebClient(ExternalApiProperties properties) {
        WebClient.Builder builder = WebClient.builder()
                .baseUrl(properties.github().baseUrl())
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json");

        if (properties.github().token() != null && !properties.github().token().isBlank()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.github().token());
        }

        return builder.build();
    }

    @Bean
    @Qualifier("hackerNewsWebClient")
    public WebClient hackerNewsWebClient(ExternalApiProperties properties) {
        return WebClient.builder()
                .baseUrl(properties.hackernews().baseUrl())
                .build();
    }

    @Bean
    @Qualifier("devToWebClient")
    public WebClient devToWebClient(ExternalApiProperties properties) {
        return WebClient.builder()
                .baseUrl(properties.devto().baseUrl())
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .build();
    }

    @Bean
    @Qualifier("geminiWebClient")
    public WebClient geminiWebClient(ExternalApiProperties properties) {
        WebClient.Builder builder = WebClient.builder()
                .baseUrl(properties.gemini().baseUrl())
                .defaultHeader(HttpHeaders.ACCEPT, "application/json");

        if (properties.gemini().apiKey() != null && !properties.gemini().apiKey().isBlank()) {
            builder.defaultHeader("x-goog-api-key", properties.gemini().apiKey());
        }

        return builder.build();
    }
}
