package com.todaydev.auth.service;

import com.todaydev.auth.security.JwtClaims;
import com.todaydev.auth.security.JwtProvider;
import com.todaydev.common.config.RedisKeyFactory;
import com.todaydev.common.exception.ErrorCode;
import com.todaydev.common.exception.TodaydevException;
import java.time.Duration;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class RefreshTokenService {

    private final ReactiveStringRedisTemplate redisTemplate;
    private final RedisKeyFactory redisKeyFactory;
    private final JwtProvider jwtProvider;

    public RefreshTokenService(
            ReactiveStringRedisTemplate redisTemplate,
            RedisKeyFactory redisKeyFactory,
            JwtProvider jwtProvider
    ) {
        this.redisTemplate = redisTemplate;
        this.redisKeyFactory = redisKeyFactory;
        this.jwtProvider = jwtProvider;
    }

    public Mono<Void> save(Long userId, String refreshToken) {
        String key = redisKeyFactory.refreshToken(userId);
        Duration ttl = Duration.ofSeconds(jwtProvider.refreshTokenExpiresIn());
        return redisTemplate.opsForValue()
                .set(key, refreshToken, ttl)
                .then();
    }

    public Mono<JwtClaims> verify(String refreshToken) {
        JwtClaims claims = jwtProvider.parseRefreshToken(refreshToken);
        String key = redisKeyFactory.refreshToken(claims.userId());

        return redisTemplate.opsForValue()
                .get(key)
                .filter(refreshToken::equals)
                .switchIfEmpty(Mono.error(new TodaydevException(ErrorCode.AUTH_REFRESH_TOKEN_INVALID)))
                .thenReturn(claims);
    }

    public Mono<Void> delete(Long userId) {
        return redisTemplate.delete(redisKeyFactory.refreshToken(userId)).then();
    }
}
