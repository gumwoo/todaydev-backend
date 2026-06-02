package com.todaydev.briefing.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.todaydev.external.ExternalArticle;
import com.todaydev.external.ExternalSource;
import com.todaydev.preference.domain.InterestKeyword;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class KeywordMatcherTest {

    private final KeywordMatcher matcher = new KeywordMatcher();

    @Test
    void match_findsKeywordsFromTitleSummaryAndUrlIgnoringCase() {
        ExternalArticle article = new ExternalArticle(
                ExternalSource.DEVTO,
                "1",
                "Spring WebFlux Tips",
                "https://example.com/reactive",
                "A Java reactive guide",
                OffsetDateTime.now(),
                Map.of()
        );

        List<InterestKeyword> keywords = List.of(
                new InterestKeyword(1L, 1L, "webflux", 7, null),
                new InterestKeyword(2L, 1L, "java", 3, null),
                new InterestKeyword(3L, 1L, "kotlin", 5, null)
        );

        assertThat(matcher.match(article, keywords))
                .extracting("keyword")
                .containsExactly("webflux", "java");
    }
}
