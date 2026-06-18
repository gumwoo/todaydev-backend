package com.todaydev.common.config;

import com.todaydev.common.config.properties.NotificationProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class NotificationWebClientConfig {

    @Bean
    @Qualifier("slackWebClient")
    public WebClient slackWebClient(NotificationProperties properties) {
        return WebClient.builder()
                .baseUrl(properties.slack().baseUrl())
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .build();
    }

    @Bean
    @Qualifier("discordWebClient")
    public WebClient discordWebClient(NotificationProperties properties) {
        return WebClient.builder()
                .baseUrl(properties.discord().baseUrl())
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .build();
    }
}
