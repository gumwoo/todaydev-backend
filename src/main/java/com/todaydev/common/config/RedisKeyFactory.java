package com.todaydev.common.config;

import com.todaydev.common.config.properties.RedisKeyProperties;

public class RedisKeyFactory {

    private final RedisKeyProperties properties;

    public RedisKeyFactory(RedisKeyProperties properties) {
        this.properties = properties;
    }

    public String refreshToken(Long userId) {
        return key(properties.refreshToken().prefix(), userId);
    }

    public String apiCache(String source, String cacheKey) {
        return key(properties.apiCache().prefix(), source, cacheKey);
    }

    public String aiSummaryCache(String cacheKey) {
        return key(properties.aiSummaryCache().prefix(), cacheKey);
    }

    public String progressBuffer(Long briefingId) {
        return key(properties.progressBuffer().prefix(), briefingId);
    }

    public String streamToken(String tokenId) {
        return key(properties.streamToken().prefix(), tokenId);
    }

    public String briefingInProgress(Long userId) {
        return key(properties.briefingInProgress().prefix(), userId);
    }

    private String key(String prefix, Object... parts) {
        StringBuilder builder = new StringBuilder(prefix);
        for (Object part : parts) {
            builder.append(':').append(part);
        }
        return builder.toString();
    }
}
