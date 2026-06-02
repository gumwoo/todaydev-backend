package com.todaydev.briefing.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.todaydev.briefing.domain.BriefingCandidate;
import com.todaydev.briefing.domain.Source;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BriefingDeduplicatorTest {

    private final BriefingDeduplicator deduplicator = new BriefingDeduplicator();

    @Test
    void deduplicate_keepsHighestScoredCandidateForSameUrlAndSortsByScore() {
        BriefingCandidate lowScore = candidate("1", "https://example.com/post", "10.00");
        BriefingCandidate highScore = candidate("2", "HTTPS://EXAMPLE.COM/POST ", "30.00");
        BriefingCandidate other = candidate("3", "https://example.com/other", "20.00");

        List<BriefingCandidate> result = deduplicator.deduplicate(List.of(lowScore, highScore, other));

        assertThat(result).hasSize(2);
        assertThat(result).extracting("externalId").containsExactly("2", "3");
    }

    private BriefingCandidate candidate(String externalId, String url, String score) {
        return new BriefingCandidate(
                Source.DEVTO,
                externalId,
                "Title",
                url,
                null,
                OffsetDateTime.now(),
                Map.of(),
                List.of(),
                false,
                new BigDecimal(score)
        );
    }
}
