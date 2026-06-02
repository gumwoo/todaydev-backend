package com.todaydev.external.devto;

import com.todaydev.common.config.properties.ExternalApiProperties;
import com.todaydev.common.exception.ErrorCode;
import com.todaydev.external.ExternalArticle;
import com.todaydev.external.ExternalClientSupport;
import com.todaydev.external.ExternalSource;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

@Component
public class DevToClient {

    private final WebClient webClient;
    private final ExternalClientSupport support;

    public DevToClient(
            @Qualifier("devToWebClient") WebClient webClient,
            ExternalApiProperties properties
    ) {
        this.webClient = webClient;
        this.support = new ExternalClientSupport(properties.client());
    }

    public Flux<ExternalArticle> fetchArticlesByTag(String tag, int limit) {
        int normalizedLimit = support.normalizedLimit(limit);

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/articles")
                        .queryParam("tag", normalizeTag(tag))
                        .queryParam("per_page", normalizedLimit)
                        .build())
                .retrieve()
                .onStatus(status -> status.isError(),
                        response -> support.mapStatus(response, ErrorCode.EXTERNAL_DEVTO_FAILED))
                .bodyToFlux(DevToArticleResponse.class)
                .map(this::toArticle)
                .timeout(support.timeout())
                .retryWhen(support.retrySpec())
                .onErrorMap(throwable -> support.mapUnexpected(throwable, ErrorCode.EXTERNAL_DEVTO_FAILED));
    }

    private String normalizeTag(String tag) {
        return tag == null ? "" : tag.trim().toLowerCase();
    }

    private ExternalArticle toArticle(DevToArticleResponse response) {
        List<String> tags = response.tagList() == null ? List.of() : response.tagList();

        return new ExternalArticle(
                ExternalSource.DEVTO,
                response.id() == null ? null : String.valueOf(response.id()),
                response.title(),
                response.url(),
                response.description(),
                response.publishedAt(),
                Map.of(
                        "tags", tags,
                        "reactions", response.publicReactionsCount() == null ? 0 : response.publicReactionsCount(),
                        "comments", response.commentsCount() == null ? 0 : response.commentsCount()
                )
        );
    }
}
