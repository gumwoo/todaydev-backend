package com.todaydev.common.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "briefing")
public record BriefingProperties(
        @Valid Scoring scoring,
        @Valid Collection collection,
        @Valid Ai ai
) {

    public record Scoring(
            @Min(0) int keywordWeightMultiplier,
            @Min(0) int watchedRepositoryBonus,
            @Min(0) int githubBaseScore,
            @Min(0) int hackerNewsBaseScore,
            @Min(0) int devtoBaseScore,
            @Min(1) int recencyHalfLifeHours
    ) {
    }

    public record Collection(
            @Min(1) int githubReleaseLimitPerRepository,
            @Min(1) int hackerNewsStoryLimit,
            @Min(1) int devtoArticleLimitPerKeyword,
            @Min(1) int candidateConcurrency
    ) {
    }

    public record Ai(
            @Min(1) int summaryItemLimit
    ) {
    }
}
