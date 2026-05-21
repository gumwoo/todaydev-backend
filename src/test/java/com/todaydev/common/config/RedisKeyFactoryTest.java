package com.todaydev.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.todaydev.common.config.properties.RedisKeyProperties;
import org.junit.jupiter.api.Test;

class RedisKeyFactoryTest {

    private final RedisKeyFactory keyFactory = new RedisKeyFactory(new RedisKeyProperties(
            new RedisKeyProperties.KeySpec("refresh", 1209600),
            new RedisKeyProperties.KeySpec("api-cache", 1800),
            new RedisKeyProperties.KeySpec("ai-summary", 86400),
            new RedisKeyProperties.KeySpec("progress", 600),
            new RedisKeyProperties.KeySpec("stream-token", 180),
            new RedisKeyProperties.KeySpec("briefing-inprogress", 600)
    ));

    @Test
    void createsNamespacedRedisKeys() {
        assertThat(keyFactory.refreshToken(1L)).isEqualTo("refresh:1");
        assertThat(keyFactory.apiCache("github", "spring")).isEqualTo("api-cache:github:spring");
        assertThat(keyFactory.aiSummaryCache("briefing-1")).isEqualTo("ai-summary:briefing-1");
        assertThat(keyFactory.progressBuffer(100L)).isEqualTo("progress:100");
        assertThat(keyFactory.streamToken("token-1")).isEqualTo("stream-token:token-1");
        assertThat(keyFactory.briefingInProgress(1L)).isEqualTo("briefing-inprogress:1");
    }
}
