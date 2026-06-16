package com.todaydev.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.todaydev.common.config.RedisKeyFactory;
import com.todaydev.common.config.properties.RedisKeyProperties;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class ExternalApiCacheTest {

    @Test
    @SuppressWarnings("unchecked")
    void cachedFlux_treatsCachedEmptyListAsHit() {
        ReactiveStringRedisTemplate redisTemplate = mock(ReactiveStringRedisTemplate.class);
        ReactiveValueOperations<String, String> valueOperations = mock(ReactiveValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(Mono.just("[]"));

        AtomicInteger loaderCalls = new AtomicInteger();
        ExternalApiCache cache = new ExternalApiCache(
                redisTemplate,
                new RedisKeyFactory(redisKeyProperties()),
                redisKeyProperties(),
                new ObjectMapper()
        );

        StepVerifier.create(cache.cachedFlux("GITHUB", "empty-releases", String.class, () -> {
                    loaderCalls.incrementAndGet();
                    return Flux.just("should-not-load");
                }))
                .verifyComplete();

        assertThat(loaderCalls).hasValue(0);
        verify(valueOperations, never()).set(anyString(), anyString());
    }

    private RedisKeyProperties redisKeyProperties() {
        RedisKeyProperties.KeySpec spec = new RedisKeyProperties.KeySpec("test", 60);
        return new RedisKeyProperties(spec, spec, spec, spec, spec, spec);
    }
}
