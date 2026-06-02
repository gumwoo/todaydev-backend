package com.todaydev.briefing.service;

import com.todaydev.briefing.domain.BriefingCandidate;
import com.todaydev.briefing.domain.MatchedKeyword;
import com.todaydev.briefing.domain.Source;
import com.todaydev.common.config.properties.BriefingProperties;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BriefingScorer {

    private final BriefingProperties.Scoring scoring;
    private final Clock clock;

    @Autowired
    public BriefingScorer(BriefingProperties properties) {
        this(properties.scoring(), Clock.systemUTC());
    }

    BriefingScorer(BriefingProperties.Scoring scoring, Clock clock) {
        this.scoring = scoring;
        this.clock = clock;
    }

    public BigDecimal score(BriefingCandidate candidate) {
        double score = baseScore(candidate.source())
                + keywordScore(candidate)
                + watchedRepositoryScore(candidate)
                + recencyScore(candidate.publishedAt());

        return BigDecimal.valueOf(score)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private int baseScore(Source source) {
        return switch (source) {
            case GITHUB -> scoring.githubBaseScore();
            case HACKER_NEWS -> scoring.hackerNewsBaseScore();
            case DEVTO -> scoring.devtoBaseScore();
            case AI -> 0;
        };
    }

    private int keywordScore(BriefingCandidate candidate) {
        return candidate.matchedKeywords()
                .stream()
                .mapToInt(MatchedKeyword::weight)
                .sum() * scoring.keywordWeightMultiplier();
    }

    private int watchedRepositoryScore(BriefingCandidate candidate) {
        return candidate.watchedRepository() ? scoring.watchedRepositoryBonus() : 0;
    }

    private double recencyScore(OffsetDateTime publishedAt) {
        if (publishedAt == null) {
            return 0;
        }

        long ageHours = Math.max(0, Duration.between(publishedAt.toInstant(), clock.instant()).toHours());
        double halfLives = (double) ageHours / scoring.recencyHalfLifeHours();
        return 20.0 / Math.pow(2.0, halfLives);
    }
}
