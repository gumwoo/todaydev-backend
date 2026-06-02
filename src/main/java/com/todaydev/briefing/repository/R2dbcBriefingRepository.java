package com.todaydev.briefing.repository;

import com.todaydev.briefing.domain.ApiCallLog;
import com.todaydev.briefing.domain.Briefing;
import com.todaydev.briefing.domain.BriefingItem;
import com.todaydev.briefing.domain.BriefingStatus;
import com.todaydev.briefing.domain.Source;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class R2dbcBriefingRepository implements BriefingRepository {

    private final DatabaseClient databaseClient;

    public R2dbcBriefingRepository(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
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
                .concatMap(item -> databaseClient.sql("""
                                INSERT INTO briefing_item (briefing_id, source, external_id, title, url, summary, score)
                                VALUES (:briefingId, :source, :externalId, :title, :url, :summary, :score)
                                RETURNING item_id, briefing_id, source, external_id, title, url, summary, score
                                """)
                        .bind("briefingId", briefingId)
                        .bind("source", item.source().name())
                        .bind("externalId", item.externalId())
                        .bind("title", item.title())
                        .bind("url", item.url())
                        .bind("summary", item.summary() == null ? "" : item.summary())
                        .bind("score", item.score())
                        .map(this::mapItem)
                        .one());
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

    private BriefingItem mapItem(Row row, RowMetadata metadata) {
        return new BriefingItem(
                row.get("item_id", Long.class),
                row.get("briefing_id", Long.class),
                Source.valueOf(row.get("source", String.class)),
                row.get("external_id", String.class),
                row.get("title", String.class),
                row.get("url", String.class),
                row.get("summary", String.class),
                row.get("score", BigDecimal.class)
        );
    }
}
