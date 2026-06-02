package com.todaydev.briefing.service;

import com.todaydev.common.config.RedisKeyFactory;
import com.todaydev.common.config.properties.RedisKeyProperties;
import com.todaydev.common.exception.ErrorCode;
import com.todaydev.common.exception.TodaydevException;
import java.time.Duration;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class BriefingLockService {

    private final ReactiveStringRedisTemplate redisTemplate;
    private final RedisKeyFactory redisKeyFactory;
    private final RedisKeyProperties redisKeyProperties;

    public BriefingLockService(
            ReactiveStringRedisTemplate redisTemplate,
            RedisKeyFactory redisKeyFactory,
            RedisKeyProperties redisKeyProperties
    ) {
        this.redisTemplate = redisTemplate;
        this.redisKeyFactory = redisKeyFactory;
        this.redisKeyProperties = redisKeyProperties;
    }

    public Mono<BriefingLock> acquire(Long userId) {
        String key = redisKeyFactory.briefingInProgress(userId);
        Duration ttl = Duration.ofSeconds(redisKeyProperties.briefingInProgress().ttlSeconds());

        return redisTemplate.opsForValue()
                .setIfAbsent(key, "1", ttl)
                .flatMap(acquired -> Boolean.TRUE.equals(acquired)
                        ? Mono.just(new BriefingLock(key))
                        : Mono.error(new TodaydevException(ErrorCode.BRIEFING_ALREADY_IN_PROGRESS)));
    }

    public Mono<Void> release(BriefingLock lock) {
        return redisTemplate.delete(lock.key()).then();
    }

    public record BriefingLock(String key) {
    }
}
