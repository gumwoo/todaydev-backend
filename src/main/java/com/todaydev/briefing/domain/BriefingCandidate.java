package com.todaydev.briefing.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record BriefingCandidate(
        Source source,
        String externalId,
        String title,
        String url,
        String summary,
        OffsetDateTime publishedAt,
        Map<String, Object> metadata,
        List<MatchedKeyword> matchedKeywords,
        boolean watchedRepository,
        BigDecimal score
) {

    public BriefingCandidate {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        matchedKeywords = matchedKeywords == null ? List.of() : List.copyOf(matchedKeywords);
        score = score == null ? BigDecimal.ZERO : score;
    }

    public BriefingCandidate withScore(BigDecimal newScore) {
        return new BriefingCandidate(
                source,
                externalId,
                title,
                url,
                summary,
                publishedAt,
                metadata,
                matchedKeywords,
                watchedRepository,
                newScore
        );
    }

    public BriefingItem toItem(Long briefingId) {
        return new BriefingItem(
                null,
                briefingId,
                source,
                externalId,
                title,
                url,
                summary,
                score,
                toLocalDateTime(publishedAt),
                metadata
        );
    }

    private LocalDateTime toLocalDateTime(OffsetDateTime value) {
        return value == null ? null : value.toLocalDateTime();
    }
}
