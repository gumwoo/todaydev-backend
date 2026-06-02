package com.todaydev.briefing.progress;

import com.todaydev.briefing.repository.BriefingRepository;
import com.todaydev.common.config.RedisKeyFactory;
import com.todaydev.common.config.properties.RedisKeyProperties;
import com.todaydev.common.exception.ErrorCode;
import com.todaydev.common.exception.TodaydevException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class StreamTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final RedisScript<String> CONSUME_TOKEN_SCRIPT = RedisScript.of(
            "local value = redis.call('GET', KEYS[1]); "
                    + "if value then redis.call('DEL', KEYS[1]); end; "
                    + "return value;",
            String.class
    );

    private final BriefingRepository briefingRepository;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final RedisKeyFactory redisKeyFactory;
    private final long ttlSeconds;

    public StreamTokenService(
            BriefingRepository briefingRepository,
            ReactiveStringRedisTemplate redisTemplate,
            RedisKeyFactory redisKeyFactory,
            RedisKeyProperties redisKeyProperties
    ) {
        this.briefingRepository = briefingRepository;
        this.redisTemplate = redisTemplate;
        this.redisKeyFactory = redisKeyFactory;
        this.ttlSeconds = redisKeyProperties.streamToken().ttlSeconds();
    }

    public Mono<StreamTokenResponse> issue(Long briefingId, Long userId) {
        return briefingRepository.findByIdAndUserId(briefingId, userId)
                .switchIfEmpty(Mono.error(new TodaydevException(ErrorCode.BRIEFING_NOT_FOUND)))
                .flatMap(ignored -> {
                    String token = newToken();
                    return redisTemplate.opsForValue()
                            .set(redisKeyFactory.streamToken(token), briefingId.toString(),
                                    Duration.ofSeconds(ttlSeconds))
                            .filter(Boolean.TRUE::equals)
                            .switchIfEmpty(Mono.error(new TodaydevException(ErrorCode.INTERNAL_SERVER_ERROR)))
                            .thenReturn(new StreamTokenResponse(token, ttlSeconds));
                });
    }

    public Mono<Void> consume(Long briefingId, String token) {
        if (token == null || token.isBlank()) {
            return Mono.error(new TodaydevException(ErrorCode.STREAM_TOKEN_INVALID));
        }

        return redisTemplate
                .execute(CONSUME_TOKEN_SCRIPT, List.of(redisKeyFactory.streamToken(token)))
                .singleOrEmpty()
                .switchIfEmpty(Mono.error(new TodaydevException(ErrorCode.STREAM_TOKEN_EXPIRED)))
                .filter(briefingId.toString()::equals)
                .switchIfEmpty(Mono.error(new TodaydevException(ErrorCode.STREAM_TOKEN_INVALID)))
                .then();
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
