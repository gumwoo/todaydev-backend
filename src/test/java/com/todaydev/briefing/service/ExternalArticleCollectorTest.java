package com.todaydev.briefing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.todaydev.briefing.domain.Source;
import com.todaydev.common.config.properties.BriefingProperties;
import com.todaydev.external.ExternalArticle;
import com.todaydev.external.ExternalSource;
import com.todaydev.external.devto.DevToClient;
import com.todaydev.external.github.GitHubClient;
import com.todaydev.external.github.GitHubRepositoryReference;
import com.todaydev.external.hackernews.HackerNewsClient;
import com.todaydev.preference.domain.InterestKeyword;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

class ExternalArticleCollectorTest {

    private final GitHubClient gitHubClient = mock(GitHubClient.class);
    private final HackerNewsClient hackerNewsClient = mock(HackerNewsClient.class);
    private final DevToClient devToClient = mock(DevToClient.class);

    @Test
    void collect_fetchesGithubReleasesFromKeywordSearchWhenWatchedRepositoriesAreEmpty() {
        ExternalArticle githubRelease = article(ExternalSource.GITHUB, "release-1");
        ExternalArticle devToArticle = article(ExternalSource.DEVTO, "devto-1");
        ExternalArticle hackerNewsArticle = article(ExternalSource.HACKER_NEWS, "hn-1");
        BriefingProperties properties = new BriefingProperties(
                new BriefingProperties.Scoring(10, 30, 20, 15, 12, 48),
                new BriefingProperties.Collection(5, 2, 20, 5, 8),
                new BriefingProperties.Ai(10)
        );
        ExternalArticleCollector collector = new ExternalArticleCollector(
                gitHubClient,
                hackerNewsClient,
                devToClient,
                properties
        );

        when(gitHubClient.searchRepositoriesByKeyword("spring", 2)).thenReturn(Flux.just(
                new GitHubRepositoryReference("spring-projects", "spring-framework")
        ));
        when(gitHubClient.fetchRepositoryReleases("spring-projects", "spring-framework", 5))
                .thenReturn(Flux.just(githubRelease));
        when(hackerNewsClient.fetchTopStories(20)).thenReturn(Flux.just(hackerNewsArticle));
        when(devToClient.fetchArticlesByTag("spring", 5)).thenReturn(Flux.just(devToArticle));

        StepVerifier.create(collector.collect(
                        100L,
                        List.of(new InterestKeyword(1L, 1L, "spring", 5, null)),
                        List.of()
                ))
                .assertNext(results -> {
                    SourceCollectionResult github = result(results, Source.GITHUB);
                    SourceCollectionResult devto = result(results, Source.DEVTO);
                    SourceCollectionResult hackerNews = result(results, Source.HACKER_NEWS);

                    assertThat(github.articles()).containsExactly(githubRelease);
                    assertThat(devto.articles()).containsExactly(devToArticle);
                    assertThat(hackerNews.articles()).containsExactly(hackerNewsArticle);
                })
                .verifyComplete();
    }

    private SourceCollectionResult result(List<SourceCollectionResult> results, Source source) {
        return results.stream()
                .filter(result -> result.source() == source)
                .findFirst()
                .orElseThrow();
    }

    private ExternalArticle article(ExternalSource source, String externalId) {
        return new ExternalArticle(
                source,
                externalId,
                "Spring article",
                "https://example.com/" + externalId,
                "summary",
                OffsetDateTime.now(),
                Map.of("owner", "spring-projects", "repoName", "spring-framework")
        );
    }
}
