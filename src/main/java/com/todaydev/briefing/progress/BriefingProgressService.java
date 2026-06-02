package com.todaydev.briefing.progress;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.todaydev.common.config.RedisKeyFactory;
import com.todaydev.common.config.properties.RedisKeyProperties;
import com.todaydev.common.exception.ErrorCode;
import com.todaydev.common.exception.TodaydevException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

@Service
public class BriefingProgressService {

    private final ReactiveStringRedisTemplate redisTemplate;
    private final RedisKeyFactory redisKeyFactory;
    private final Duration bufferTtl;
    private final ObjectMapper objectMapper;
    private final Map<Long, Sinks.Many<BriefingStreamMessage>> activeStreams = new ConcurrentHashMap<>();

    public BriefingProgressService(
            ReactiveStringRedisTemplate redisTemplate,
            RedisKeyFactory redisKeyFactory,
            RedisKeyProperties redisKeyProperties,
            ObjectMapper objectMapper
    ) {
        this.redisTemplate = redisTemplate;
        this.redisKeyFactory = redisKeyFactory;
        this.bufferTtl = Duration.ofSeconds(redisKeyProperties.progressBuffer().ttlSeconds());
        this.objectMapper = objectMapper;
    }

    public Mono<Void> publish(BriefingStreamMessage message) {
        String key = redisKeyFactory.progressBuffer(message.payload().briefingId());
        String json = serialize(message);

        return redisTemplate.opsForList().rightPush(key, json)
                .then(redisTemplate.expire(key, bufferTtl))
                .doOnSuccess(ignored -> {
                    Long briefingId = message.payload().briefingId();
                    Sinks.Many<BriefingStreamMessage> activeStream = sink(briefingId);
                    activeStream.tryEmitNext(message);
                    if (message.terminal()) {
                        activeStreams.remove(briefingId, activeStream);
                    }
                })
                .then();
    }

    public Flux<BriefingStreamMessage> stream(Long briefingId) {
        String key = redisKeyFactory.progressBuffer(briefingId);
        Flux<BriefingStreamMessage> buffered = redisTemplate.opsForList()
                .range(key, 0, -1)
                .map(this::deserialize);
        Flux<BriefingStreamMessage> live = sink(briefingId).asFlux();

        return buffered.concatWith(live)
                .distinct(message -> message.payload().step())
                .takeUntil(BriefingStreamMessage::terminal)
                .switchIfEmpty(Flux.error(new TodaydevException(ErrorCode.STREAM_NOT_FOUND)));
    }

    private Sinks.Many<BriefingStreamMessage> sink(Long briefingId) {
        return activeStreams.computeIfAbsent(briefingId,
                ignored -> Sinks.many().multicast().onBackpressureBuffer());
    }

    private String serialize(BriefingStreamMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException exception) {
            throw new TodaydevException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private BriefingStreamMessage deserialize(String json) {
        try {
            return objectMapper.readValue(json, BriefingStreamMessage.class);
        } catch (JsonProcessingException exception) {
            throw new TodaydevException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
