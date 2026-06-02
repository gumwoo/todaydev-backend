package com.todaydev.briefing.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.todaydev.briefing.domain.BriefingCandidate;
import com.todaydev.briefing.domain.MatchedKeyword;
import com.todaydev.briefing.domain.Source;
import com.todaydev.common.config.properties.BriefingProperties;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BriefingScorerTest {

    @Test
    void score_combinesSourceKeywordRepositoryAndRecencyScores() {
        BriefingProperties.Scoring scoring = new BriefingProperties.Scoring(10, 30, 20, 15, 12, 48);
        Clock clock = Clock.fixed(Instant.parse("2026-05-22T00:00:00Z"), ZoneOffset.UTC);
        BriefingScorer scorer = new BriefingScorer(scoring, clock);

        BriefingCandidate candidate = new BriefingCandidate(
                Source.GITHUB,
                "1",
                "Spring release",
                "https://example.com",
                null,
                OffsetDateTime.parse("2026-05-22T00:00:00Z"),
                Map.of(),
                List.of(new MatchedKeyword("spring", 5)),
                true,
                BigDecimal.ZERO
        );

        assertThat(scorer.score(candidate)).isEqualByComparingTo("120.00");
    }
}
