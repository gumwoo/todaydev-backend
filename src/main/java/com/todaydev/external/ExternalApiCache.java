package com.todaydev.external;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.todaydev.common.config.RedisKeyFactory;
import com.todaydev.common.config.properties.RedisKeyProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class ExternalApiCache {

    private static final Logger log = LoggerFactory.getLogger(ExternalApiCache.class);

    private final ReactiveStringRedisTemplate redisTemplate;
    private final RedisKeyFactory redisKeyFactory;
    private final ObjectMapper objectMapper;
    private final Duration ttl;

    public ExternalApiCache(
            ReactiveStringRedisTemplate redisTemplate,
            RedisKeyFactory redisKeyFactory,
            RedisKeyProperties redisKeyProperties,
            ObjectMapper objectMapper
    ) {
        this.redisTemplate = redisTemplate;
        this.redisKeyFactory = redisKeyFactory;
        this.objectMapper = objectMapper;
        this.ttl = Duration.ofSeconds(redisKeyProperties.apiCache().ttlSeconds());
        log.info("외부 API 캐시 초기화 완료: ttlSeconds={}", ttl.toSeconds());
    }

    public <T> Flux<T> cachedFlux(String source, String cacheKey, Class<T> elementType, Supplier<Flux<T>> loader) {
        String redisKey = redisKeyFactory.apiCache(source, hashed(cacheKey));

        return redisTemplate.opsForValue()
                .get(redisKey)
                .flatMap(cached -> decode(cached, elementType))
                .map(CacheLookup::hit)
                .defaultIfEmpty(CacheLookup.miss())
                .flatMapMany(lookup -> lookup.hit()
                        ? cachedItems(source, redisKey, lookup.items())
                        : loadAndCache(source, redisKey, elementType, loader))
                .onErrorResume(throwable -> {
                    log.warn("외부 API 캐시 처리 실패, 원본 API 호출로 대체: source={}, key={}", source, redisKey, throwable);
                    return Flux.defer(loader);
                });
    }

    private <T> Flux<T> loadAndCache(String source, String redisKey, Class<T> elementType, Supplier<Flux<T>> loader) {
        return Flux.defer(loader)
                .collectList()
                .flatMapMany(items -> write(redisKey, items)
                        .doOnNext(saved -> log.debug(
                                "외부 API 캐시 미적중: source={}, key={}, saved={}, expired={}, size={}",
                                source,
                                redisKey,
                                saved.written(),
                                saved.expired(),
                                items.size()
                        ))
                        .thenMany(Flux.fromIterable(items)));
    }

    private <T> Flux<T> cachedItems(String source, String redisKey, List<T> items) {
        log.debug("외부 API 캐시 적중: source={}, key={}, size={}", source, redisKey, items.size());
        return Flux.fromIterable(items);
    }

    private <T> Mono<CacheWriteResult> write(String redisKey, List<T> items) {
        try {
            return redisTemplate.opsForValue()
                    .set(redisKey, objectMapper.writeValueAsString(items))
                    .flatMap(saved -> redisTemplate.expire(redisKey, ttl)
                            .map(expired -> new CacheWriteResult(saved, expired)));
        } catch (JsonProcessingException exception) {
            log.warn("외부 API 캐시 직렬화 실패: key={}, size={}", redisKey, items.size(), exception);
            return Mono.just(new CacheWriteResult(false, false));
        }
    }

    private record CacheWriteResult(boolean written, boolean expired) {
    }

    private record CacheLookup<T>(boolean hit, List<T> items) {

        private static <T> CacheLookup<T> hit(List<T> items) {
            return new CacheLookup<>(true, items);
        }

        private static <T> CacheLookup<T> miss() {
            return new CacheLookup<>(false, List.of());
        }
    }

    private <T> Mono<List<T>> decode(String cached, Class<T> elementType) {
        try {
            JavaType type = objectMapper.getTypeFactory().constructCollectionType(List.class, elementType);
            List<T> items = objectMapper.readValue(cached, type);
            return Mono.just(items);
        } catch (JsonProcessingException exception) {
            return Mono.empty();
        }
    }

    private String hashed(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is not available", exception);
        }
    }
}
