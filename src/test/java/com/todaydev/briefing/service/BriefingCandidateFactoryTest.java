package com.todaydev.briefing.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.todaydev.common.config.properties.BriefingProperties;
import com.todaydev.external.ExternalArticle;
import com.todaydev.external.ExternalSource;
import com.todaydev.preference.domain.InterestKeyword;
import com.todaydev.preference.domain.WatchedRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BriefingCandidateFactoryTest {

    @Test
    void create_mapsExternalArticleToScoredCandidate() {
        BriefingProperties properties = new BriefingProperties(
                new BriefingProperties.Scoring(10, 30, 20, 15, 12, 48),
                new BriefingProperties.Collection(5, 3, 20, 5, 8),
                new BriefingProperties.Ai(10)
        );
        BriefingCandidateFactory factory = new BriefingCandidateFactory(
                new KeywordMatcher(),
                new WatchedRepositoryMatcher(),
                new BriefingScorer(properties)
        );

        ExternalArticle article = new ExternalArticle(
                ExternalSource.GITHUB,
                "100",
                "Spring WebFlux release",
                "https://github.com/spring-projects/spring-framework/releases/100",
                "Reactive improvements",
                OffsetDateTime.now(),
                Map.of("owner", "spring-projects", "repoName", "spring-framework")
        );

        var candidate = factory.create(
                article,
                List.of(new InterestKeyword(1L, 1L, "webflux", 7, null)),
                List.of(new WatchedRepository(1L, 1L, "spring-projects", "spring-framework", null))
        );

        assertThat(candidate.source().name()).isEqualTo("GITHUB");
        assertThat(candidate.matchedKeywords()).extracting("keyword").containsExactly("webflux");
        assertThat(candidate.watchedRepository()).isTrue();
        assertThat(candidate.score()).isPositive();
    }
}
