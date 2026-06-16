package com.todaydev.external.hackernews;

import com.todaydev.common.config.properties.ExternalApiProperties;
import com.todaydev.common.exception.ErrorCode;
import com.todaydev.external.ExternalApiCache;
import com.todaydev.external.ExternalArticle;
import com.todaydev.external.ExternalClientSupport;
import com.todaydev.external.ExternalSource;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class HackerNewsClient {

    private static final int ITEM_CONCURRENCY = 8;

    private final WebClient webClient;
    private final ExternalClientSupport support;
    private final ExternalApiCache cache;

    public HackerNewsClient(
            @Qualifier("hackerNewsWebClient") WebClient webClient,
            ExternalApiProperties properties,
            ExternalApiCache cache
    ) {
        this.webClient = webClient;
        this.support = new ExternalClientSupport(properties.client());
        this.cache = cache;
    }

    public Flux<ExternalArticle> fetchTopStories(int limit) {
        int normalizedLimit = support.normalizedLimit(limit);

        return cache.cachedFlux(
                ExternalSource.HACKER_NEWS.name(),
                "topstories:%d".formatted(normalizedLimit),
                ExternalArticle.class,
                () -> fetchTopStoriesFromApi(normalizedLimit)
        );
    }

    private Flux<ExternalArticle> fetchTopStoriesFromApi(int normalizedLimit) {
        return webClient.get()
                .uri("/topstories.json")
                .retrieve()
                .onStatus(status -> status.isError(),
                        response -> support.mapStatus(response, ErrorCode.EXTERNAL_HACKER_NEWS_FAILED))
                .bodyToMono(new ParameterizedTypeReference<List<Long>>() {
                })
                .flatMapMany(ids -> Flux.fromIterable(ids).take(normalizedLimit))
                .flatMap(this::fetchItem, ITEM_CONCURRENCY)
                .filter(item -> item.title() != null && !item.title().isBlank())
                .map(this::toArticle)
                .timeout(support.timeout())
                .retryWhen(support.retrySpec())
                .onErrorMap(throwable -> support.mapUnexpected(throwable, ErrorCode.EXTERNAL_HACKER_NEWS_FAILED));
    }

    private Mono<HackerNewsItemResponse> fetchItem(Long itemId) {
        return webClient.get()
                .uri("/item/{itemId}.json", itemId)
                .retrieve()
                .onStatus(status -> status.isError(),
                        response -> support.mapStatus(response, ErrorCode.EXTERNAL_HACKER_NEWS_FAILED))
                .bodyToMono(HackerNewsItemResponse.class);
    }

    private ExternalArticle toArticle(HackerNewsItemResponse response) {
        String itemUrl = response.url() == null || response.url().isBlank()
                ? "https://news.ycombinator.com/item?id=" + response.id()
                : response.url();

        return new ExternalArticle(
                ExternalSource.HACKER_NEWS,
                response.id() == null ? null : String.valueOf(response.id()),
                response.title(),
                itemUrl,
                null,
                response.publishedAt(),
                Map.of(
                        "author", response.by() == null ? "" : response.by(),
                        "score", response.score() == null ? 0 : response.score(),
                        "type", response.type() == null ? "" : response.type()
                )
        );
    }
}
