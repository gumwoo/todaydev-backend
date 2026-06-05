package com.todaydev.external.github;

import com.todaydev.common.config.properties.ExternalApiProperties;
import com.todaydev.common.exception.ErrorCode;
import com.todaydev.external.ExternalArticle;
import com.todaydev.external.ExternalClientSupport;
import com.todaydev.external.ExternalSource;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

@Component
public class GitHubClient {

    private final WebClient webClient;
    private final ExternalClientSupport support;

    public GitHubClient(
            @Qualifier("githubWebClient") WebClient webClient,
            ExternalApiProperties properties
    ) {
        this.webClient = webClient;
        this.support = new ExternalClientSupport(properties.client());
    }

    public Flux<ExternalArticle> fetchRepositoryReleases(String owner, String repoName, int limit) {
        int normalizedLimit = support.normalizedLimit(limit);

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/repos/{owner}/{repo}/releases")
                        .queryParam("per_page", normalizedLimit)
                        .build(owner, repoName))
                .retrieve()
                .onStatus(status -> status.isError(),
                        response -> support.mapStatus(response, ErrorCode.EXTERNAL_GITHUB_FAILED))
                .bodyToFlux(GitHubReleaseResponse.class)
                .map(response -> toArticle(owner, repoName, response))
                .timeout(support.timeout())
                .retryWhen(support.retrySpec())
                .onErrorMap(throwable -> support.mapUnexpected(throwable, ErrorCode.EXTERNAL_GITHUB_FAILED));
    }

    public Flux<GitHubRepositoryReference> searchRepositoriesByKeyword(String keyword, int limit) {
        String normalizedKeyword = normalizeKeyword(keyword);

        if (normalizedKeyword.isBlank()) {
            return Flux.empty();
        }

        int normalizedLimit = support.normalizedLimit(limit);

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search/repositories")
                        .queryParam("q", normalizedKeyword)
                        .queryParam("sort", "stars")
                        .queryParam("order", "desc")
                        .queryParam("per_page", normalizedLimit)
                        .build())
                .retrieve()
                .onStatus(status -> status.isError(),
                        response -> support.mapStatus(response, ErrorCode.EXTERNAL_GITHUB_FAILED))
                .bodyToMono(GitHubRepositorySearchResponse.class)
                .flatMapMany(response -> Flux.fromIterable(response.items() == null ? List.of() : response.items()))
                .map(GitHubRepositorySearchResponse.Item::toReference)
                .filter(GitHubRepositoryReference::valid)
                .timeout(support.timeout())
                .retryWhen(support.retrySpec())
                .onErrorMap(throwable -> support.mapUnexpected(throwable, ErrorCode.EXTERNAL_GITHUB_FAILED));
    }

    private ExternalArticle toArticle(String owner, String repoName, GitHubReleaseResponse response) {
        return new ExternalArticle(
                ExternalSource.GITHUB,
                response.externalId(),
                response.displayTitle(),
                response.htmlUrl(),
                response.body(),
                response.publishedAt(),
                Map.of(
                        "owner", owner,
                        "repoName", repoName,
                        "tagName", response.tagName() == null ? "" : response.tagName()
                )
        );
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
    }
}
