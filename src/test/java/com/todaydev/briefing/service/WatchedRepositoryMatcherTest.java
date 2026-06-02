package com.todaydev.briefing.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.todaydev.external.ExternalArticle;
import com.todaydev.external.ExternalSource;
import com.todaydev.preference.domain.WatchedRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WatchedRepositoryMatcherTest {

    private final WatchedRepositoryMatcher matcher = new WatchedRepositoryMatcher();

    @Test
    void matches_returnsTrueForGithubArticleWithWatchedRepositoryMetadata() {
        ExternalArticle article = new ExternalArticle(
                ExternalSource.GITHUB,
                "100",
                "Release",
                "https://github.com/spring-projects/spring-framework/releases/1",
                null,
                OffsetDateTime.now(),
                Map.of("owner", "spring-projects", "repoName", "spring-framework")
        );

        List<WatchedRepository> repositories = List.of(
                new WatchedRepository(1L, 1L, "Spring-Projects", "Spring-Framework", null)
        );

        assertThat(matcher.matches(article, repositories)).isTrue();
    }

    @Test
    void matches_returnsFalseForNonGithubArticle() {
        ExternalArticle article = new ExternalArticle(
                ExternalSource.DEVTO,
                "10",
                "Spring article",
                "https://dev.to/example",
                null,
                OffsetDateTime.now(),
                Map.of("owner", "spring-projects", "repoName", "spring-framework")
        );

        assertThat(matcher.matches(article, List.of(
                new WatchedRepository(1L, 1L, "spring-projects", "spring-framework", null)
        ))).isFalse();
    }
}
