package com.todaydev.common.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.redis.keys")
public record RedisKeyProperties(
        @Valid KeySpec refreshToken,
        @Valid KeySpec apiCache,
        @Valid KeySpec aiSummaryCache,
        @Valid KeySpec progressBuffer,
        @Valid KeySpec streamToken,
        @Valid KeySpec briefingInProgress
) {

    public record KeySpec(
            @NotBlank String prefix,
            @Min(1) long ttlSeconds
    ) {
    }
}
