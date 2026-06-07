package com.todaydev.briefing.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.todaydev.briefing.domain.ApiCallLog;
import com.todaydev.briefing.domain.Briefing;
import com.todaydev.briefing.domain.BriefingItem;
import com.todaydev.briefing.domain.BriefingStatus;
import com.todaydev.briefing.domain.Source;
import com.todaydev.common.exception.ErrorCode;
import com.todaydev.common.exception.TodaydevException;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class R2dbcBriefingRepository implements BriefingRepository {

    private static final TypeReference<Map<String, Object>> METADATA_TYPE = new TypeReference<>() {
    };

    private final DatabaseClient databaseClient;
    private final ObjectMapper objectMapper;

    public R2dbcBriefingRepository(DatabaseClient databaseClient, ObjectMapper objectMapper) {
        this.databaseClient = databaseClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Briefing> createGenerating(Long userId) {
        return databaseClient.sql("""
                        INSERT INTO briefing (user_id, status)
                        VALUES (:userId, :status)
                        RETURNING briefing_id, user_id, title, summary, status, generated_at
                        """)
                .bind("userId", userId)
                .bind("status", BriefingStatus.GENERATING.name())
                .map(this::mapBriefing)
                .one();
    }

    @Override
    public Mono<Briefing> findByIdAndUserId(Long briefingId, Long userId) {
        return databaseClient.sql("""
                        SELECT briefing_id, user_id, title, summary, status, generated_at
                        FROM briefing
                        WHERE briefing_id = :briefingId
                          AND user_id = :userId
                        """)
                .bind("briefingId", briefingId)
                .bind("userId", userId)
                .map(this::mapBriefing)
                .one();
    }

    @Override
    public Flux<BriefingListItem> findByUserId(Long userId, int page, int size) {
        return databaseClient.sql("""
                        SELECT b.briefing_id, b.title, b.summary, b.status, b.generated_at,
                               COUNT(bi.item_id) AS item_count
                        FROM briefing b
                        LEFT JOIN briefing_item bi ON bi.briefing_id = b.briefing_id
                        WHERE b.user_id = :userId
                        GROUP BY b.briefing_id, b.title, b.summary, b.status, b.generated_at
                        ORDER BY b.generated_at DESC, b.briefing_id DESC
                        LIMIT :limit OFFSET :offset
                        """)
                .bind("userId", userId)
                .bind("limit", size)
                .bind("offset", page * size)
                .map(this::mapBriefingListItem)
                .all();
    }

    @Override
    public Mono<Long> countByUserId(Long userId) {
        return databaseClient.sql("""
                        SELECT COUNT(*) AS total_count
                        FROM briefing
                        WHERE user_id = :userId
                        """)
                .bind("userId", userId)
                .map(row -> row.get("total_count", Long.class))
                .one();
    }

    @Override
    public Mono<Boolean> existsByUserIdAndGeneratedAtBetween(Long userId, LocalDateTime start, LocalDateTime end) {
        return databaseClient.sql("""
                        SELECT EXISTS (
                            SELECT 1
                            FROM briefing
                            WHERE user_id = :userId
                              AND generated_at >= :start
                              AND generated_at < :end
                        ) AS exists
                        """)
                .bind("userId", userId)
                .bind("start", start)
                .bind("end", end)
                .map(row -> Boolean.TRUE.equals(row.get("exists", Boolean.class)))
                .one();
    }

    @Override
    public Flux<BriefingItemDetail> findItemsByBriefingIdAndUserId(Long briefingId, Long userId) {
        return databaseClient.sql("""
                        SELECT bi.item_id, bi.source, bi.external_id, bi.title, bi.url,
                               bi.summary, bi.score, COALESCE(bi.published_at, b.generated_at) AS published_at,
                               bi.metadata::text AS metadata,
                               CASE WHEN sa.saved_id IS NULL THEN false ELSE true END AS saved
                        FROM briefing_item bi
                        JOIN briefing b ON b.briefing_id = bi.briefing_id
                        LEFT JOIN saved_article sa
                               ON sa.item_id = bi.item_id
                              AND sa.user_id = :userId
                        WHERE bi.briefing_id = :briefingId
                          AND b.user_id = :userId
                        ORDER BY bi.score DESC, bi.item_id ASC
                        """)
                .bind("briefingId", briefingId)
                .bind("userId", userId)
                .map(this::mapItemDetail)
                .all();
    }

    @Override
    public Mono<Briefing> updateStatus(Long briefingId, BriefingStatus status, String title, String summary) {
        return databaseClient.sql("""
                        UPDATE briefing
                        SET status = :status,
                            title = :title,
                            summary = :summary
                        WHERE briefing_id = :briefingId
                        RETURNING briefing_id, user_id, title, summary, status, generated_at
                        """)
                .bind("briefingId", briefingId)
                .bind("status", status.name())
                .bind("title", title)
                .bind("summary", summary)
                .map(this::mapBriefing)
                .one();
    }

    @Override
    public Flux<BriefingItem> saveItems(Long briefingId, Iterable<BriefingItem> items) {
        return Flux.fromIterable(items)
                .concatMap(item -> {
                    DatabaseClient.GenericExecuteSpec spec = databaseClient.sql("""
                                    INSERT INTO briefing_item (
                                        briefing_id, source, external_id, title, url, summary,
                                        score, published_at, metadata
                                    )
                                    VALUES (
                                        :briefingId, :source, :externalId, :title, :url, :summary,
                                        :score, :publishedAt, CAST(:metadata AS jsonb)
                                    )
                                    RETURNING item_id, briefing_id, source, external_id, title, url,
                                              summary, score, published_at, metadata::text AS metadata
                                    """)
                            .bind("briefingId", briefingId)
                            .bind("source", item.source().name())
                            .bind("externalId", item.externalId())
                            .bind("title", item.title())
                            .bind("url", item.url())
                            .bind("summary", item.summary() == null ? "" : item.summary())
                            .bind("score", item.score())
                            .bind("metadata", serializeMetadata(item.metadata()));

                    if (item.publishedAt() == null) {
                        spec = spec.bindNull("publishedAt", LocalDateTime.class);
                    } else {
                        spec = spec.bind("publishedAt", item.publishedAt());
                    }

                    return spec.map(this::mapItem).one();
                });
    }

    @Override
    public Mono<Void> saveApiCallLogs(Iterable<ApiCallLog> logs) {
        return Flux.fromIterable(logs)
                .concatMap(log -> databaseClient.sql("""
                                INSERT INTO api_call_log (briefing_id, source, status, latency_ms, error_message)
                                VALUES (:briefingId, :source, :status, :latencyMs, :errorMessage)
                                """)
                        .bind("briefingId", log.briefingId())
                        .bind("source", log.source().name())
                        .bind("status", log.status().name())
                        .bind("latencyMs", log.latencyMs() == null ? 0 : log.latencyMs())
                        .bind("errorMessage", log.errorMessage() == null ? "" : log.errorMessage())
                        .fetch()
                        .rowsUpdated())
                .then();
    }

    private Briefing mapBriefing(Row row, RowMetadata metadata) {
        return new Briefing(
                row.get("briefing_id", Long.class),
                row.get("user_id", Long.class),
                row.get("title", String.class),
                row.get("summary", String.class),
                BriefingStatus.valueOf(row.get("status", String.class)),
                row.get("generated_at", LocalDateTime.class)
        );
    }

    private BriefingListItem mapBriefingListItem(Row row, RowMetadata metadata) {
        return new BriefingListItem(
                row.get("briefing_id", Long.class),
                row.get("title", String.class),
                row.get("summary", String.class),
                BriefingStatus.valueOf(row.get("status", String.class)),
                row.get("generated_at", LocalDateTime.class),
                row.get("item_count", Long.class)
        );
    }

    private BriefingItem mapItem(Row row, RowMetadata metadata) {
        return new BriefingItem(
                row.get("item_id", Long.class),
                row.get("briefing_id", Long.class),
                Source.valueOf(row.get("source", String.class)),
                row.get("external_id", String.class),
                row.get("title", String.class),
                row.get("url", String.class),
                row.get("summary", String.class),
                row.get("score", BigDecimal.class),
                row.get("published_at", LocalDateTime.class),
                deserializeMetadata(row.get("metadata", String.class))
        );
    }

    private BriefingItemDetail mapItemDetail(Row row, RowMetadata metadata) {
        return new BriefingItemDetail(
                row.get("item_id", Long.class),
                Source.valueOf(row.get("source", String.class)),
                row.get("external_id", String.class),
                row.get("title", String.class),
                row.get("url", String.class),
                row.get("summary", String.class),
                row.get("score", BigDecimal.class),
                row.get("published_at", LocalDateTime.class),
                deserializeMetadata(row.get("metadata", String.class)),
                Boolean.TRUE.equals(row.get("saved", Boolean.class))
        );
    }

    private String serializeMetadata(Map<String, Object> metadata) {
        try {
            return objectMapper.writeValueAsString(metadata == null ? Map.of() : metadata);
        } catch (JsonProcessingException exception) {
            throw new TodaydevException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private Map<String, Object> deserializeMetadata(String metadata) {
        if (metadata == null || metadata.isBlank()) {
            return Map.of();
        }

        try {
            return objectMapper.readValue(metadata, METADATA_TYPE);
        } catch (JsonProcessingException exception) {
            throw new TodaydevException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
