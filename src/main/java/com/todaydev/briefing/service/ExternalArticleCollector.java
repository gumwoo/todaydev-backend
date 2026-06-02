package com.todaydev.briefing.service;

import com.todaydev.briefing.domain.ApiCallLog;
import com.todaydev.briefing.domain.Source;
import com.todaydev.common.config.properties.BriefingProperties;
import com.todaydev.external.ExternalArticle;
import com.todaydev.external.devto.DevToClient;
import com.todaydev.external.github.GitHubClient;
import com.todaydev.external.hackernews.HackerNewsClient;
import com.todaydev.preference.domain.InterestKeyword;
import com.todaydev.preference.domain.WatchedRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class ExternalArticleCollector {

    private final GitHubClient gitHubClient;
    private final HackerNewsClient hackerNewsClient;
    private final DevToClient devToClient;
    private final BriefingProperties.Collection collection;

    public ExternalArticleCollector(
            GitHubClient gitHubClient,
            HackerNewsClient hackerNewsClient,
            DevToClient devToClient,
            BriefingProperties properties
    ) {
        this.gitHubClient = gitHubClient;
        this.hackerNewsClient = hackerNewsClient;
        this.devToClient = devToClient;
        this.collection = properties.collection();
    }

    public Mono<List<SourceCollectionResult>> collect(
            Long briefingId,
            List<InterestKeyword> keywords,
            List<WatchedRepository> repositories
    ) {
        return Flux.merge(
                        collectGitHub(briefingId, repositories),
                        collectHackerNews(briefingId),
                        collectDevTo(briefingId, keywords)
                )
                .collectList();
    }

    private Mono<SourceCollectionResult> collectGitHub(Long briefingId, List<WatchedRepository> repositories) {
        Instant startedAt = Instant.now();

        return Flux.fromIterable(repositories)
                .flatMap(repository -> gitHubClient.fetchRepositoryReleases(
                        repository.owner(),
                        repository.repoName(),
                        collection.githubReleaseLimitPerRepository()
                ))
                .collectList()
                .map(articles -> success(briefingId, Source.GITHUB, startedAt, articles))
                .onErrorResume(throwable -> Mono.just(failed(briefingId, Source.GITHUB, startedAt)));
    }

    private Mono<SourceCollectionResult> collectHackerNews(Long briefingId) {
        Instant startedAt = Instant.now();

        return hackerNewsClient.fetchTopStories(collection.hackerNewsStoryLimit())
                .collectList()
                .map(articles -> success(briefingId, Source.HACKER_NEWS, startedAt, articles))
                .onErrorResume(throwable -> Mono.just(failed(briefingId, Source.HACKER_NEWS, startedAt)));
    }

    private Mono<SourceCollectionResult> collectDevTo(Long briefingId, List<InterestKeyword> keywords) {
        Instant startedAt = Instant.now();

        return Flux.fromIterable(keywords)
                .flatMap(keyword -> devToClient.fetchArticlesByTag(
                        keyword.keyword(),
                        collection.devtoArticleLimitPerKeyword()
                ))
                .collectList()
                .map(articles -> success(briefingId, Source.DEVTO, startedAt, articles))
                .onErrorResume(throwable -> Mono.just(failed(briefingId, Source.DEVTO, startedAt)));
    }

    private SourceCollectionResult success(
            Long briefingId,
            Source source,
            Instant startedAt,
            List<ExternalArticle> articles
    ) {
        return new SourceCollectionResult(
                source,
                articles,
                ApiCallLog.success(briefingId, source, latencyMs(startedAt))
        );
    }

    private SourceCollectionResult failed(Long briefingId, Source source, Instant startedAt) {
        return new SourceCollectionResult(
                source,
                List.of(),
                ApiCallLog.failed(briefingId, source, latencyMs(startedAt))
        );
    }

    private long latencyMs(Instant startedAt) {
        return Duration.between(startedAt, Instant.now()).toMillis();
    }
}
